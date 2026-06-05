#include "hardware/oled.h"
#include "config.h"
#include "thm_logo.h"

namespace hw::oled {

static Adafruit_SH1106 g_display(OLED_RESET_PIN);

void begin() {
  g_display.begin(0x02, OLED_I2C_ADDRESS, OLED_RESET_PIN);
  g_display.clearDisplay();
  g_display.setRotation(0);
  g_display.display();

  g_display.setTextColor(WHITE);
  g_display.setFont(&FreeSans9pt7b);
}

void showThmLogo() {
  g_display.clearDisplay();

  delay(1000);

  g_display.drawBitmap(
      (g_display.width() - THM_LOGO_WIDTH) / 2,
      (g_display.height() - THM_LOGO_HEIGHT) / 2,
      THM_LOGO_BITMAP, THM_LOGO_WIDTH, THM_LOGO_HEIGHT, 1);

  g_display.setCursor(0, 60);
  g_display.display();
}

Adafruit_SH1106& display() {
  return g_display;
}

} // namespace hw::oled
