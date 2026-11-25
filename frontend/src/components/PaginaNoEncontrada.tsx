import { Button, Center, Stack, Text, Title } from '@mantine/core';
import { Link } from 'react-router';

export function PaginaNoEncontrada() {
  return (
    <Center mih="100vh" p="md">
      <Stack align="center" gap="sm">
        <Title order={1} fz={28}>
          Página no encontrada
        </Title>
        <Text c="dimmed">La dirección que abrió no existe.</Text>
        <Button component={Link} to="/" mt="sm">
          Ir al inicio
        </Button>
      </Stack>
    </Center>
  );
}
