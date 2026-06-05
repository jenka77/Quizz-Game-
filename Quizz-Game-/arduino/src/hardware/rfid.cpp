#include "hardware/rfid.h"
#include "config.h"

namespace hw::rfid {

static MFRC522 g_rfid(PIN_RFID_CS, PIN_RFID_RST);
static bool g_readerDetected = false;
static uint32_t g_lastDetectCheckMs = 0;
static uint32_t g_lastReadMs = 0;
static String g_lastUid;

void begin() {

  SPI.begin();

  pinMode(PIN_RFID_CS, OUTPUT);
  g_rfid.PCD_Init();

  if (detectReaderOnce()) {
#if SERIAL_DEBUG
    Serial.println("RFID reader detected.");
#endif
    g_readerDetected = true;
  } else {
#if SERIAL_DEBUG
    Serial.println("RFID reader NOT detected (check wiring/SPI/CS/RST).");
#endif
    g_readerDetected = false;
  }

#if SERIAL_DEBUG
  g_rfid.PCD_DumpVersionToSerial();
#endif
}

void service() {
  const uint32_t now = millis();

  // Check if cooldown time passed, return otherwise.
  if (!(now - g_lastDetectCheckMs >= RFID_DETECT_INTERVAL_MS)) return;

  const bool detectedNow = detectReaderOnce();
  if (detectedNow) g_lastDetectCheckMs = now;

  if (!g_readerDetected && detectedNow) {
    g_readerDetected = true;
#if SERIAL_DEBUG
    Serial.println("RFID reader detected. Scanning for cards...");
#endif
    g_rfid.PCD_Init();
#if SERIAL_DEBUG
    g_rfid.PCD_DumpVersionToSerial();
#endif
  }

  if (g_readerDetected && !detectedNow) {
    g_readerDetected = false;
#if SERIAL_DEBUG
    Serial.println("RFID reader is no longer reachable.");
#endif
  }

  // If reader not available, return.
  if (!g_readerDetected) return;

  // If no card available, return.
  if (!(g_rfid.PICC_IsNewCardPresent() && g_rfid.PICC_ReadCardSerial())) return;

  const String currentUid = uidToString(g_rfid.uid);
  Serial.println("RFID: " + currentUid);  // Immer im Hardware-Terminal sichtbar
  g_lastUid = currentUid;
  g_lastReadMs = now;

  g_rfid.PICC_HaltA();
  g_rfid.PCD_StopCrypto1();
}

bool isReaderDetected() {
  return g_readerDetected;
}

const String& lastUid() {
  return g_lastUid;
}

bool detectReaderOnce() {
  byte v = g_rfid.PCD_ReadRegister(MFRC522::VersionReg);
  // If 0x00 or 0xFF the read probably failed.
  return !(v == 0x00 || v == 0xFF);
}

String uidToString(const MFRC522::Uid &uid) {
  String out;
  // Erwartetes Format seitens Frontend/DB: "04 9C 64 D2 45 2B 80"
  out.reserve(uid.size * 3);
  for (byte i = 0; i < uid.size; i++) {
    if (i > 0) out += ' ';
    if (uid.uidByte[i] < 0x10) out += '0';
    out += String(uid.uidByte[i], HEX);
  }
  out.toUpperCase(); // ensures 9C (not 9c)
  return out;
}

} // namespace hw::rfid
