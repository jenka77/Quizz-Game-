#pragma once

#include <Arduino.h>
#include <SPI.h>
#include <MFRC522.h>

namespace hw::rfid {

// Initialize SPI and the RC522 RFID-reader.
void begin();

// Has to be called in `loop()` regularly, as this handles the card detection and scans.
void service();

// Returns true, if the reader is reachable, false otherwise.
bool isReaderDetected();

// Returns the last read UID as HEX-string (e.g. "04A1...").
const String& lastUid();

// Helper to convert a uid to a HEX-string.
String uidToString(const MFRC522::Uid &uid);

// Helper to init reader. Returns true on success, false otherwise.
bool detectReaderOnce();

} // namespace hw::rfid
