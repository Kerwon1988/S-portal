# Presupuesto de rendimiento

Los valores son presupuesto inicial de desarrollo, no SLA contractual.

| Métrica | Gate inicial |
|---|---:|
| Error rate | 0 % en smoke; < 1 % en carga preparada |
| HTTP p95 | ≤ 800 ms |
| HTTP p99 | ≤ 1.500 ms |
| Throughput | registrar, sin mínimo contractual |
| Lighthouse performance móvil | ≥ 90 (provisional) |
| LCP | ≤ 2,5 s; CLS ≤ 0,1; INP ≤ 200 ms |

Carga aislada, datos sintéticos, sin DEMO. El SLA/concurrencia reales siguen pendientes de aprobación; un resultado no puede reinterpretarse como capacidad productiva. CPU/DB p95/p99 requieren collector y baseline de PRE.

Estado de carga/Lighthouse/DB: **PREPARADO_NO_EJECUTADO** por falta de entorno arrancado/autorizado.
