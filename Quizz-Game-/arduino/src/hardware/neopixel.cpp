#include "hardware/neopixel.h"
#include "config.h"

namespace hw::neopixel {

static Adafruit_NeoPixel g_strip(NEOPIXEL_LED_COUNT, PIN_NEOPIXEL, NEO_RGB + NEO_KHZ800);

void begin() {
  g_strip.begin();
  g_strip.setBrightness(200);
  g_strip.show();
}

void setAll(uint32_t color) {
  for (uint16_t i = 0; i < g_strip.numPixels(); i++) {
    g_strip.setPixelColor(i, color);
  }
  g_strip.show();
}

void off() {
  setAll(g_strip.Color(0, 0, 0));
}

void flash(uint32_t color, uint16_t waitMs) {
  setAll(color);
  delay(waitMs);
}

Adafruit_NeoPixel& strip() {
  return g_strip;
}

} // namespace hw::neopixel
