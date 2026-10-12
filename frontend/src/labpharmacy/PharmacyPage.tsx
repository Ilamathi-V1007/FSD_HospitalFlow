import { useQuery } from '@tanstack/react-query';
import { api } from '../common/api';
import { formatDateTime } from '../common/format';
import { useAction } from '../common/hooks';
import type { Prescription } from '../common/types';
import { Button, Card, Empty, ErrorNote, PageHeader, Spinner } from '../common/ui';

// The server does not publish live events for prescriptions yet, so poll instead.
const REFRESH_MS = 15_000;

export default function PharmacyPage() {
  const pending = useQuery({
    queryKey: ['prescriptions', 'PENDING'],
    queryFn: () => api<Prescription[]>('/api/prescriptions?status=PENDING'),
    refetchInterval: REFRESH_MS,
  });
  const dispensed = useQuery({
    queryKey: ['prescriptions', 'DISPENSED'],
    queryFn: () => api<Prescription[]>('/api/prescriptions?status=DISPENSED'),
    refetchInterval: REFRESH_MS,
  });

  const dispense = useAction((id: number) => api<Prescription>(`/api/prescriptions/${id}/dispense`, { method: 'POST' }), {
    success: (p) => `${p.medicine} dispensed for ${p.patientName}`,
  });

  const recent = [...(dispensed.data ?? [])]
    .sort((a, b) => (b.dispensedAt ?? '').localeCompare(a.dispensedAt ?? ''))
    .slice(0, 10);

  return (
    <>
      <PageHeader title="Prescriptions" subtitle="Prescriptions written by doctors, oldest first. Mark each one when it has been handed to the patient." />

      <div className="grid gap-5 lg:grid-cols-2">
        <Card title={`To dispense${pending.data ? ` (${pending.data.length})` : ''}`}>
          {pending.isLoading && <Spinner />}
          {pending.isError && <ErrorNote message="Could not load prescriptions." />}
          {pending.data?.length === 0 && <Empty title="Nothing to dispense">New prescriptions appear here shortly after a doctor adds them.</Empty>}
          <ul className="space-y-3">
            {pending.data?.map((p) => (
              <li key={p.id} className="rounded border border-slate-200 p-3">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-medium text-slate-900">{p.patientName}</p>
                    <p className="mt-0.5 text-sm text-slate-800">
                      {p.medicine} <span className="text-slate-600">{p.dosage}</span>
                    </p>
                    {p.instructions && <p className="mt-0.5 text-sm text-slate-600">{p.instructions}</p>}
                    <p className="mt-1 text-xs text-slate-500">
                      Prescribed by {p.prescribedByName} · {formatDateTime(p.prescribedAt)}
                    </p>
                  </div>
                  <Button loading={dispense.isPending && dispense.variables === p.id} onClick={() => dispense.mutate(p.id)}>
                    Mark dispensed
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        </Card>

        <Card title="Dispensed recently">
          {dispensed.data?.length === 0 && <p className="text-sm text-slate-500">Nothing dispensed yet.</p>}
          <ul className="space-y-3">
            {recent.map((p) => (
              <li key={p.id} className="text-sm">
                <p className="font-medium text-slate-900">
                  {p.medicine} <span className="font-normal text-slate-500">· {p.patientName}</span>
                </p>
                <p className="text-slate-700">{p.dosage}</p>
                <p className="text-xs text-slate-500">
                  By {p.dispensedByName ?? 'pharmacist'} · {p.dispensedAt ? formatDateTime(p.dispensedAt) : ''}
                </p>
              </li>
            ))}
          </ul>
        </Card>
      </div>
    </>
  );
}
