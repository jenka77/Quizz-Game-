#pragma once

#include <Arduino.h>
#include <Adafruit_GFX.h>
#include <Fonts/FreeSans9pt7b.h>
#include "Adafruit_SH1106.hpp"

namespace hw::oled {

// Initialize the OLED.
void begin();

// Display the THM-Logo.
void showThmLogo();

// Direct access to the underlying display data.
Adafruit_SH1106& display();

} // namespace hw::oled
