#pragma once

#include <Arduino.h>
#include <Adafruit_NeoPixel.h>

namespace hw::neopixel {

// Starts the NeoPixel-Strip.
void begin();

// Sets all LEDs to the same color.
void setAll(uint32_t color);

// Turns off all LEDs.
void off();

// Convenience function to flash all LEDs for a short period of time.
void flash(uint32_t color, uint16_t waitMs);

// Direct access to the underlying strip data.
Adafruit_NeoPixel& strip();

} // namespace hw::neopixel
