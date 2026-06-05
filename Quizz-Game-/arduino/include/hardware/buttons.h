#pragma once

#include <Arduino.h>

namespace hw::buttons {

// Initialize the button pins.
void begin();

// Returns true, if the button is pressed, falser otherwise.
bool isPressed(uint8_t pin);

} // namespace hw::buttons
