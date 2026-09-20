import { notFound } from "next/navigation";
import Dashboard from "../../../../components/dashboard";
import { positiveId, sections, type Screen } from "../../../../lib/navigation";
export default async function CitizenPage({
  params,
}: {
  params: Promise<{ userId: string; section?: string[] }>;
}) {
  const route = await params;
  const userId = positiveId(route.userId);
  if (!userId) notFound();
  const segments = route.section || [];
  const path = segments.join("/");
  const screen = (Object.keys(sections) as (keyof typeof sections)[]).find(
    (key) => sections[key] === path,
  );
  if (screen) return <Dashboard routeUserId={userId} screen={screen} />;
  if (segments.length === 2 && segments[0] === "exames") {
    const examId = positiveId(segments[1]);
    if (examId)
      return (
        <Dashboard
          routeUserId={userId}
          screen={"detail" satisfies Screen}
          routeExamId={examId}
        />
      );
  }
  notFound();
}
