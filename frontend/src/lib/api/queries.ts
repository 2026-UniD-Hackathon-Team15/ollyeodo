"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "./client";

export const queryKeys = {
  menus: ["menus"] as const,
  check: (id: number) => ["checks", id] as const,
  rules: ["rules"] as const,
  stats: ["checks", "stats"] as const,
};

export function useMenus() {
  return useQuery({ queryKey: queryKeys.menus, queryFn: api.listMenus });
}

export function useCheck(id: number) {
  return useQuery({
    queryKey: queryKeys.check(id),
    queryFn: () => api.getCheck(id),
    enabled: Number.isFinite(id),
  });
}

export function useRules() {
  return useQuery({ queryKey: queryKeys.rules, queryFn: api.listRules });
}

export function useCreateMenu() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: api.createMenu,
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.menus }),
  });
}

export function useCheckStats() {
  return useQuery({ queryKey: queryKeys.stats, queryFn: api.getStats });
}

export function useCreateCheck() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: api.createCheck,
    onSuccess: (result) => qc.setQueryData(queryKeys.check(result.checkId), result),
  });
}
