#pragma once

#include <Arduino.h>

// =======================
// Pin mapping
// =======================

// NeoPixel
static constexpr uint8_t PIN_NEOPIXEL = D2;
static constexpr uint16_t NEOPIXEL_LED_COUNT = 4;

// Buttons
static constexpr uint8_t PIN_BTN_GREEN  = D4;
static constexpr uint8_t PIN_BTN_RED    = D5;
static constexpr uint8_t PIN_BTN_YELLOW = D6;
static constexpr uint8_t PIN_BTN_BLUE   = D7;

// OLED (I2C)
static constexpr uint8_t PIN_OLED_SDA = A4;
static constexpr uint8_t PIN_OLED_SCL = A5;

// RFID (RC522, SPI)
// SCK=D13, SS/SDA=D10, MOSI=D11, MISO=D12, RST=D9
static constexpr uint8_t PIN_RFID_RST  = D9;
static constexpr uint8_t PIN_RFID_CS   = D10;
static constexpr uint8_t PIN_RFID_MOSI = D11;
static constexpr uint8_t PIN_RFID_MISO = D12;
static constexpr uint8_t PIN_RFID_SCK  = D13;

// =======================
// Hardware behavior
// =======================

static constexpr bool BUTTON_ACTIVE_LOW = true;      // Button against GND -> INPUT_PULLUP
static constexpr uint32_t RFID_READ_INTERVAL_MS = 5000;
static constexpr uint32_t RFID_DETECT_INTERVAL_MS = 500; // Check every 0.5s

// =======================
// OLED config
// =======================

static constexpr uint8_t OLED_I2C_ADDRESS = 0x3C;
static constexpr int OLED_RESET_PIN = -1;

// Screen size (SH1106/SSD1306 compatible)
static constexpr uint16_t SCREEN_WIDTH  = 128;
static constexpr uint16_t SCREEN_HEIGHT = 64;

// Serial-Debug: RFID-UID im Terminal anzeigen (#define für #if Präprozessor)
#define SERIAL_DEBUG 1
// Serial-Debug: MQTT-Publikationen ausgegeben (LEDs/Buttons trotzdem aktiv)
#define SERIAL_DEBUG_MQTT 1
