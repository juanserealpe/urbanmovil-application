import { Alert, Button, Center, Stack } from '@mantine/core';
import { IconAlertCircle } from '@tabler/icons-react';

interface Props {
  mensaje: string;
  onReintentar?: () => void;
}

/** Error al cargar datos, con opción de reintentar. */
export function ErrorPantalla({ mensaje, onReintentar }: Props) {
  return (
    <Center mih="40vh" p="md">
      <Stack align="center" maw={480}>
        <Alert color="red" variant="light" icon={<IconAlertCircle size={18} />} title="No se pudo cargar" role="alert">
          {mensaje}
        </Alert>
        {onReintentar && (
          <Button variant="default" onClick={onReintentar}>
            Reintentar
          </Button>
        )}
      </Stack>
    </Center>
  );
}
