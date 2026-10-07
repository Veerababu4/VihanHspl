import type { ClinicalNote } from '../../../api/clinical'

/** "BP 130/85 · Pulse 78 · Temp 98.6 °F · SpO₂ 98% · Wt 72.5 kg" (only what was recorded). */
export function vitalsLine(c: ClinicalNote): string {
  return [
    c.bpSystolic != null && c.bpDiastolic != null ? `BP ${c.bpSystolic}/${c.bpDiastolic}` : null,
    c.pulse != null ? `Pulse ${c.pulse}` : null,
    c.temperatureF != null ? `Temp ${c.temperatureF} °F` : null,
    c.spo2 != null ? `SpO₂ ${c.spo2}%` : null,
    c.weightKg != null ? `Wt ${c.weightKg} kg` : null,
    c.heightCm != null ? `Ht ${c.heightCm} cm` : null,
  ].filter(Boolean).join(' · ')
}
