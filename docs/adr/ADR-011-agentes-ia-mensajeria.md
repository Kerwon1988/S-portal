# ADR-011 — Agentes de IA en la mensajería

- Estado: Propuesto/Futuro (R3+)
- Fecha: 2026-11-10

## Contexto
La mensajería interna puede habilitar sugerencias automatizadas, pero procesaría mensajes/datos de clientes. Riesgos: transferencia internacional, encargado, prompt injection, alucinación, exposición de PII y respuesta incorrecta en nombre de la empresa.

## Decisión propuesta
No llamar proveedor de IA en MVP. Preparar `authorType=AI_AGENT`, evento `MessageReceived` con IDs y puerto `AiReplyAssistant`; cualquier salida futura será borrador sin envío automático, requiere aprobación humana explícita y permisos de `INBOX_MANAGE`. Antes de piloto: evaluación de impacto, contrato de encargado/transferencia, minimización y allowlist de datos.

## Alternativas
- Respuesta automática directa: rechazada por riesgo de mensaje no revisado y acciones incorrectas.
- Proveedor fuera del EEE sin garantías/análisis: rechazado.
- No integrar ni dejar puerto: menor complejidad, pero se pierde punto de extensión; conservar interfaz sin implementación ni dependencia de proveedor.

## Consecuencias
Diseñar aislamiento de prompts/datos, neutralizar instrucciones de mensajes, registrar aprobación/rechazo, limitar herramientas y validar salida. No almacenar prompts/respuestas completos en logs. El proveedor, región y modelo no se presuponen; «DeepSeek» es ejemplo de idea, no selección.

## Verificaciones pendientes
- MSG-012: solo modelo/puerto/evento sin ejecución.
- R3: evaluación RGPD/DPIA, región, DPA, seguridad de prompt injection y control humano.
- HRD-008: añadir proveedor al RAT y lista de encargados antes de transferencia.
