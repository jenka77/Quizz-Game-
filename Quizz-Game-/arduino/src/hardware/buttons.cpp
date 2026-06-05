#include "hardware/buttons.h"
#include "config.h"

namespace hw::buttons {

void begin() {
  // Buttons with Pullup -> aktiv LOW
  if (BUTTON_ACTIVE_LOW) {
    pinMode(PIN_BTN_BLUE, INPUT_PULLUP);
    pinMode(PIN_BTN_YELLOW, INPUT_PULLUP);
    pinMode(PIN_BTN_GREEN, INPUT_PULLUP);
    pinMode(PIN_BTN_RED, INPUT_PULLUP);
  // Buttons without Pullup -> aktiv HIGH
  } else {
    pinMode(PIN_BTN_BLUE, INPUT);
    pinMode(PIN_BTN_YELLOW, INPUT);
    pinMode(PIN_BTN_GREEN, INPUT);
    pinMode(PIN_BTN_RED, INPUT);
  }
}

bool isPressed(uint8_t pin) {
  return digitalRead(pin) == HIGH;
}

} // namespace hw::buttons
