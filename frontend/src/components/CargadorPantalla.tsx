import { Center, Loader } from '@mantine/core';

export function CargadorPantalla() {
  return (
    <Center mih="60vh" role="status" aria-label="Cargando">
      <Loader />
    </Center>
  );
}
