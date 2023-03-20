package io.github.chadj2.mesh;


public class Color {
  private static final float INT_COLOR_SCALE = 1.0f / 255.0f;

  public float r;
  public float g;
  public float b;
  public float a;

  /** Construct a Color and default it to white (1, 1, 1, 1). */
  @SuppressWarnings("initialization")
  public Color() {
    setWhite();
  }

  /** Construct a Color with the values of another color. */
  @SuppressWarnings("initialization")
  public Color(Color color) {
    set(color);
  }

  /** Construct a color with the RGB values passed in and an alpha of 1. */
  @SuppressWarnings("initialization")
  public Color(float r, float g, float b) {
    set(r, g, b);
  }

  /** Construct a color with the RGBA values passed in. */
  @SuppressWarnings("initialization")
  public Color(float r, float g, float b, float a) {
    set(r, g, b, a);
  }

  /**
   * Construct a color with an integer in the sRGB color space packed as an ARGB value. Used for
   * constructing from an Android ColorInt.
   */
  @SuppressWarnings("initialization")
  public Color(int argb) {
    set(argb);
  }

  /** Set to the values of another color. */
  public void set(Color color) {
    set(color.r, color.g, color.b, color.a);
  }

  /** Set to the RGB values passed in and an alpha of 1. */
  public void set(float r, float g, float b) {
    set(r, g, b, 1.0f);
  }

  /** Set to the RGBA values passed in. */
  public void set(float r, float g, float b, float a) {
    this.r = Math.max(0.0f, Math.min(1.0f, r));
    this.g = Math.max(0.0f, Math.min(1.0f, g));
    this.b = Math.max(0.0f, Math.min(1.0f, b));
    this.a = Math.max(0.0f, Math.min(1.0f, a));
  }

  public int getRed() {
    return (int) (r * 255.0f + 0.5f);
  }

  public int getGreen() {
    return (int) (g * 255.0f + 0.5f);
  }
  public int getBlue() {
    return (int) (b * 255.0f + 0.5f);
  }
  public int getAlpha() {
    return (int) (a * 255.0f + 0.5f);
  }
  /**
   * Set to RGBA values from an integer in the sRGB color space packed as an ARGB value. Used for
   * setting from an Android ColorInt.
   */
  public void set(int argb) {
    // sRGB color
    final int red = (argb >> 16) & 0xFF;
    final int green = (argb >> 8) & 0xFF;
    final int blue = (argb) & 0xFF;
    final int alpha = argb >>> 24;

    // Convert from sRGB to linear and from

    r = ((red <= 0.04045f) ?
            (red / 12.92f) : (float) Math.pow((red + 0.055f) / 1.055f, 2.4f)) * INT_COLOR_SCALE;
    g = ((green <= 0.04045f) ?
            green / 12.92f : (float) Math.pow((green + 0.055f) / 1.055f, 2.4f)) * INT_COLOR_SCALE;
    b = ((blue <= 0.04045f) ?
            blue / 12.92f : (float) Math.pow((blue + 0.055f) / 1.055f, 2.4f)) * INT_COLOR_SCALE;
    a = (float) alpha * INT_COLOR_SCALE;
  }

  public int argb() {
    return ((int) (a * 255.0f + 0.5f) << 24) |
            ((int) (r * 255.0f + 0.5f) << 16) |
            ((int) (g * 255.0f + 0.5f) <<  8) |
            (int) (b * 255.0f + 0.5f);
  }

  public float[] getRGBComponents(float[] compArray) {
    float[] f;
    if (compArray == null) {
      f = new float[4];
    } else {
      f = compArray;
    }


    f[0] = r;
    f[1] = g;
    f[2] = b;
    f[3] = a;

    return f;
  }

  /** Sets the color to white. RGBA is (1, 1, 1, 1). */
  private void setWhite() {
    set(1.0f, 1.0f, 1.0f);
  }

  /** Returns a new color with Sceneform's tonemapping inversed. */
  public Color inverseTonemap() {
    Color color = new Color(r, g, b, a);
    color.r = inverseTonemap(r);
    color.g = inverseTonemap(g);
    color.b = inverseTonemap(b);
    return color;
  }

  private static float inverseTonemap(float val) {
    return (val * -0.155f) / (val - 1.019f);
  }

  public static int HSBtoRGB(float hue, float saturation, float brightness) {
    int r = 0;
    int g = 0;
    int b = 0;
    if (saturation == 0.0F) {
      r = g = b = (int)(brightness * 255.0F + 0.5F);
    } else {
      float h = (hue - (float)Math.floor((double)hue)) * 6.0F;
      float f = h - (float)Math.floor((double)h);
      float p = brightness * (1.0F - saturation);
      float q = brightness * (1.0F - saturation * f);
      float t = brightness * (1.0F - saturation * (1.0F - f));
      switch ((int)h) {
        case 0:
          r = (int)(brightness * 255.0F + 0.5F);
          g = (int)(t * 255.0F + 0.5F);
          b = (int)(p * 255.0F + 0.5F);
          break;
        case 1:
          r = (int)(q * 255.0F + 0.5F);
          g = (int)(brightness * 255.0F + 0.5F);
          b = (int)(p * 255.0F + 0.5F);
          break;
        case 2:
          r = (int)(p * 255.0F + 0.5F);
          g = (int)(brightness * 255.0F + 0.5F);
          b = (int)(t * 255.0F + 0.5F);
          break;
        case 3:
          r = (int)(p * 255.0F + 0.5F);
          g = (int)(q * 255.0F + 0.5F);
          b = (int)(brightness * 255.0F + 0.5F);
          break;
        case 4:
          r = (int)(t * 255.0F + 0.5F);
          g = (int)(p * 255.0F + 0.5F);
          b = (int)(brightness * 255.0F + 0.5F);
          break;
        case 5:
          r = (int)(brightness * 255.0F + 0.5F);
          g = (int)(p * 255.0F + 0.5F);
          b = (int)(q * 255.0F + 0.5F);
      }
    }

    return -16777216 | r << 16 | g << 8 | b << 0;
  }

  public static float[] RGBtoHSB(float r, float g, float b, float[] hsbvals) {
    if (hsbvals == null) {
      hsbvals = new float[3];
    }

    float cmax = r > g ? r : g;
    if (b > cmax) {
      cmax = b;
    }

    float cmin = r < g ? r : g;
    if (b < cmin) {
      cmin = b;
    }

    float brightness = cmax;
    float saturation;
    if (cmax != 0F) {
      saturation = (float)(cmax - cmin) / (float)cmax;
    } else {
      saturation = 0.0F;
    }

    float hue;
    if (saturation == 0.0F) {
      hue = 0.0F;
    } else {
      float redc = (float)(cmax - r) / (float)(cmax - cmin);
      float greenc = (float)(cmax - g) / (float)(cmax - cmin);
      float bluec = (float)(cmax - b) / (float)(cmax - cmin);
      if (r == cmax) {
        hue = bluec - greenc;
      } else if (g == cmax) {
        hue = 2.0F + redc - bluec;
      } else {
        hue = 4.0F + greenc - redc;
      }

      hue /= 6.0F;
      if (hue < 0.0F) {
        ++hue;
      }
    }

    hsbvals[0] = hue;
    hsbvals[1] = saturation;
    hsbvals[2] = brightness;
    return hsbvals;
  }

  public static Color getHSBColor(float h, float s, float b) {
    return new Color(HSBtoRGB(h, s, b));
  }
}
