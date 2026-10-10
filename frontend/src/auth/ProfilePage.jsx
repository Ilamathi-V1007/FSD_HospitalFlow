import PageShell from "../common/PageShell";

export default function ProfilePage() {
  return (
    <PageShell
      title="Profile"
      objective="Let any logged-in user manage their own account."
      functions={[
        "View own account details",
        "Edit profile info",
        "Change password",
      ]}
    />
  );
}
