import { MantineProvider } from '@mantine/core';
import { DatesProvider } from '@mantine/dates';
import { ModalsProvider } from '@mantine/modals';
import { Notifications } from '@mantine/notifications';
import { QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import '../utils/fechas'; // registra plugins y locale de dayjs
import { queryClient } from './queryClient';
import { cssVariablesResolver, theme } from './theme';

export function Providers({ children }: { children: ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      <MantineProvider theme={theme} cssVariablesResolver={cssVariablesResolver} defaultColorScheme="light">
        <DatesProvider settings={{ locale: 'es' }}>
          <Notifications position="top-right" />
          <ModalsProvider labels={{ confirm: 'Confirmar', cancel: 'Cancelar' }}>{children}</ModalsProvider>
        </DatesProvider>
      </MantineProvider>
    </QueryClientProvider>
  );
}
