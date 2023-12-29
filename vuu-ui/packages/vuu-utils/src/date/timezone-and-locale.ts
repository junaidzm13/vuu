import { logger } from "../logging-utils";

const { warn } = logger("Date/time validation");

type Type = "timeZone" | "locale";

function validateOrGetDefault(type: Type, value?: string): string {
  const { locale, options } =
    type === "locale"
      ? { locale: value, options: {} }
      : { locale: undefined, options: { [type]: value } };

  const validatedValue = (() => {
    try {
      // it either throws or falls back to the default locale/timeZone
      return Intl.DateTimeFormat(locale, options).resolvedOptions()[type];
    } catch (_) {
      // this returns the default locale/timeZone
      return Intl.DateTimeFormat().resolvedOptions()[type];
    }
  })();

  if (value !== undefined && value !== validatedValue) {
    warn?.(`Invalid ${type} ${value} passed. Falling back to user's default.`);
  }

  return validatedValue;
}

export function validateLocaleOrGetDefault(locale?: string): string {
  return validateOrGetDefault("locale", locale);
}

export function validateTimeZoneOrGetDefault(timeZone?: string): string {
  return validateOrGetDefault("timeZone", timeZone);
}

export const localeOptions = [
  "de-DE",
  "en-GB",
  "en-US",
  "ja-JP",
  "zh-Hans-CN",
] as const;

export const timeZoneOptions = [
  "America/Los_Angeles",
  "America/New_York",
  "Asia/Shanghai",
  "Asia/Tokyo",
  "Australia/Sydney",
  "Australia/Perth",
  "Europe/Berlin",
  "Europe/London",
] as const;
