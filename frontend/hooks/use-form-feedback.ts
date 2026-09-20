"use client";

import { useEffect, useRef, useState } from "react";

export function confirmNavigation() {
  return window.dispatchEvent(
    new Event("sanguebom:navigate", { cancelable: true }),
  );
}

export function useFormFeedback(error: string) {
  const formRef = useRef<HTMLFormElement>(null);
  const [dirty, setDirty] = useState(false);
  const dirtyRef = useRef(false);
  useEffect(() => {
    if (error)
      formRef.current?.querySelector<HTMLElement>('[role="alert"]')?.focus();
  }, [error]);
  useEffect(() => {
    if (!dirty) return;
    const unload = (event: BeforeUnloadEvent) => {
      if (dirtyRef.current) {
        event.preventDefault();
        event.returnValue = "";
      }
    };
    const navigate = (event: Event) => {
      if (
        dirtyRef.current &&
        !window.confirm(
          "Você tem alterações não salvas. Deseja sair sem salvar?",
        )
      )
        event.preventDefault();
    };
    const click = (event: MouseEvent) => {
      const link = (event.target as Element).closest?.(
        "a[href]",
      ) as HTMLAnchorElement | null;
      if (
        !link ||
        link.target === "_blank" ||
        event.ctrlKey ||
        event.metaKey ||
        event.shiftKey ||
        link.hash ||
        link.href === window.location.href
      )
        return;
      if (!confirmNavigation()) {
        event.preventDefault();
        event.stopPropagation();
      }
    };
    window.addEventListener("beforeunload", unload);
    window.addEventListener("sanguebom:navigate", navigate);
    document.addEventListener("click", click, true);
    return () => {
      window.removeEventListener("beforeunload", unload);
      window.removeEventListener("sanguebom:navigate", navigate);
      document.removeEventListener("click", click, true);
    };
  }, [dirty]);
  return {
    formRef,
    markDirty: () => {
      dirtyRef.current = true;
      setDirty(true);
    },
    markSaved: () => {
      dirtyRef.current = false;
      setDirty(false);
    },
  };
}
