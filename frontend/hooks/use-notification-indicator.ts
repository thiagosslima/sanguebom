"use client";

import { useCallback, useEffect, useState } from "react";

export type NotificationMarker = { id: number; createdAt: string };
export type NotificationHead = {
  userId: number;
  latest: NotificationMarker | null;
};

function validMarker(value: unknown): value is NotificationMarker {
  if (!value || typeof value !== "object") return false;
  const marker = value as NotificationMarker;
  return (
    Number.isSafeInteger(marker.id) &&
    marker.id > 0 &&
    typeof marker.createdAt === "string" &&
    Number.isFinite(Date.parse(marker.createdAt))
  );
}

export function newerNotification(
  a: NotificationMarker | null,
  b: NotificationMarker | null,
) {
  if (!validMarker(a)) return validMarker(b) ? b : null;
  if (!validMarker(b)) return a;
  const difference = Date.parse(a.createdAt) - Date.parse(b.createdAt);
  return difference > 0 || (difference === 0 && a.id >= b.id) ? a : b;
}

// The API's PENDING/SENT states track delivery, not whether the user viewed the list.
// Store only a per-citizen marker, never notification messages or medical data.
const sessionMarkers = new Map<number, NotificationMarker>();
const storageKey = (id: number) => `sanguebom-notifications-seen:${id}`;

export function useNotificationIndicator(
  userId: number | null,
  head: NotificationHead | null,
  viewing: boolean,
) {
  const [seen, setSeen] = useState<{
    userId: number;
    marker: NotificationMarker | null;
  } | null>(null);

  useEffect(() => {
    if (!userId) return;
    function load() {
      let marker = sessionMarkers.get(userId!) || null;
      try {
        const saved: unknown = JSON.parse(
          localStorage.getItem(storageKey(userId!)) || "null",
        );
        if (validMarker(saved)) marker = newerNotification(marker, saved);
      } catch {
        /* Storage can be unavailable; keep the in-memory marker. */
      }
      if (marker) sessionMarkers.set(userId!, marker);
      setSeen({ userId: userId!, marker });
    }
    load();
    const sync = (event: StorageEvent) => {
      if (event.key === storageKey(userId) || event.key === null) load();
    };
    window.addEventListener("storage", sync);
    return () => window.removeEventListener("storage", sync);
  }, [userId]);

  const latest = head?.userId === userId ? head.latest : null;
  const marker = seen?.userId === userId ? seen.marker : null;
  const hasUnseen =
    !!latest &&
    seen?.userId === userId &&
    (!marker || newerNotification(marker, latest) !== marker);

  const markSeen = useCallback(() => {
    if (!userId || !latest || !hasUnseen) return;
    const value = { id: latest.id, createdAt: latest.createdAt };
    sessionMarkers.set(userId, value);
    setSeen({ userId, marker: value });
    try {
      localStorage.setItem(storageKey(userId), JSON.stringify(value));
    } catch {}
  }, [userId, latest, hasUnseen]);

  useEffect(() => {
    if (viewing) markSeen();
  }, [viewing, markSeen]);

  return { hasUnseen, markSeen };
}
