import Navbar from "./Navbar";

export default function PageShell({ title, objective, functions, children }) {
  return (
    <div className="page-shell">
      <Navbar />
      <main className="page-content">
        <h1>{title}</h1>
        <p className="page-objective">{objective}</p>

        <ul className="page-functions">
          {functions.map((fn, i) => (
            <li key={i}>{fn}</li>
          ))}
        </ul>

        {children}
      </main>
    </div>
  );
}
