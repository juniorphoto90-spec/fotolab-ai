# FotoLab AI - MVP Android

Primeira base funcional em Kotlin + Jetpack Compose.

## O que já está implementado
- Selecionar uma foto do aparelho.
- Ajustar brilho, contraste e saturação com prévia.
- Salvar presets no armazenamento privado do app (SharedPreferences).
- Aplicar presets em outras fotos.
- Renomear, duplicar e excluir presets.
- Exportar a imagem editada para `Pictures/FotoLabAI` na galeria.
- Interface inicial para explicar a etapa de integração de IA.

## O que ainda falta
- Integração real de IA para remover objetos, trocar fundos e restaurar fotos.
- Testes em aparelhos físicos e diferentes versões de Android.
- Ícone, identidade visual final, política de privacidade e preparação para Google Play.
- Gerar e assinar um APK/AAB em um ambiente Android Studio/Gradle.

## Como abrir e executar
1. Instale o Android Studio em um computador.
2. Abra a pasta `fotolab-ai` como projeto existente.
3. Aguarde a sincronização do Gradle.
4. Execute em um emulador ou celular Android com depuração USB ativada.

Os presets ficam no aparelho de cada usuário e não são enviados para um servidor.
