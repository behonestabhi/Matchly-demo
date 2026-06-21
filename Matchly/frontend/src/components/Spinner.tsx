export function Spinner({ label }: { label?: string }) {
  return (
    <div className="flex items-center gap-3 text-gray-500">
      <span className="h-5 w-5 animate-spin rounded-full border-2 border-gray-300 border-t-brand-600" />
      {label && <span className="text-sm">{label}</span>}
    </div>
  );
}
