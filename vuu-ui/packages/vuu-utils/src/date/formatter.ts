import { isNotNullOrUndefined } from "../ts-utils";
import { DatePattern, DateTimePattern, TimePattern } from "./types";

type DateTimeFormatConfig = {
  locale?: string;
  options: Intl.DateTimeFormatOptions;
  postProcessor?: (s: string) => string;
};

// Time format config
const baseTimeFormatOptions: Intl.DateTimeFormatOptions = {
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
};
const formatConfigByTimePatterns: Record<TimePattern, DateTimeFormatConfig> = {
  "hh:mm:ss": {
    locale: "en-GB",
    options: { ...baseTimeFormatOptions, hour12: false },
  },
  "hh:mm:ss a": {
    locale: "en-GB",
    options: { ...baseTimeFormatOptions, hour12: true },
  },
};

// Date format config
const baseDateFormatOptions: Intl.DateTimeFormatOptions = {
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
};
const formatConfigByDatePatterns: Record<DatePattern, DateTimeFormatConfig> = {
  "dd.mm.yyyy": {
    locale: "en-GB",
    options: { ...baseDateFormatOptions },
    postProcessor: (s) => s.replaceAll("/", "."),
  },
  "dd/mm/yyyy": { locale: "en-GB", options: { ...baseDateFormatOptions } },
  "dd MMM yyyy": {
    locale: "en-GB",
    options: { ...baseDateFormatOptions, month: "short" },
  },
  "dd MMMM yyyy": {
    locale: "en-GB",
    options: { ...baseDateFormatOptions, month: "long" },
  },
  "mm/dd/yyyy": { locale: "en-US", options: { ...baseDateFormatOptions } },
  "MMM dd, yyyy": {
    locale: "en-US",
    options: { ...baseDateFormatOptions, month: "short" },
  },
  "MMMM dd, yyyy": {
    locale: "en-US",
    options: { ...baseDateFormatOptions, month: "long" },
  },
};

function getFormatConfigs(pattern: DateTimePattern) {
  return [
    isNotNullOrUndefined(pattern["date"])
      ? formatConfigByDatePatterns[pattern["date"]]
      : null,
    isNotNullOrUndefined(pattern["time"])
      ? formatConfigByTimePatterns[pattern["time"]]
      : null,
  ];
}

function applyFormatting(
  d: Date,
  opts: Pick<DateTimeFormatConfig, "postProcessor"> & {
    dateTimeFormat: Intl.DateTimeFormat;
  }
): string {
  const { dateTimeFormat, postProcessor } = opts;
  const dateStr = dateTimeFormat.format(d);
  return postProcessor ? postProcessor(dateStr) : dateStr;
}

export function formatDate(pattern: DateTimePattern): (d: Date) => string {
  const formattingOpts = getFormatConfigs(pattern)
    .filter(isNotNullOrUndefined)
    .map((c) => ({
      dateTimeFormat: Intl.DateTimeFormat(c.locale, c.options),
      postProcessor: c.postProcessor,
    }));

  return (d) =>
    formattingOpts.map((opts) => applyFormatting(d, opts)).join(" ");
}
