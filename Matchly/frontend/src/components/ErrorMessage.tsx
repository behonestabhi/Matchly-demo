export function ErrorMessage({ error }: { error: unknown }) {
  const message =
    error instanceof Error ? error.message : 'Something went wrong.';
  return (
    <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
      {message}
    </div>
  );
}
