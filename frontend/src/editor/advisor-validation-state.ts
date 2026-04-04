import type { AdvisorValidationEventPayload } from "./types";

interface AdvisorValidationMergeResult {
  events: AdvisorValidationEventPayload[];
  added: boolean;
}

export function createAdvisorValidationKey(event: AdvisorValidationEventPayload): string {
  const stableKey = event.stableKey.trim();

  if (stableKey.length > 0) {
    return stableKey;
  }

  return [event.documentName, event.ruleId, event.matchedText]
    .map((part) => part.trim().toLowerCase())
    .filter((part) => part.length > 0)
    .join("::");
}

export function appendUniqueAdvisorValidationEvent(
  events: readonly AdvisorValidationEventPayload[],
  incoming: AdvisorValidationEventPayload,
): AdvisorValidationMergeResult {
  const incomingKey = createAdvisorValidationKey(incoming);
  const alreadyPresent = events.some((event) => createAdvisorValidationKey(event) === incomingKey);

  return {
    events: alreadyPresent ? [...events] : [...events, incoming],
    added: !alreadyPresent,
  };
}
