import Link from "next/link";
export default function NotFound() {
  return (
    <main>
      <section className="panel empty">
        <h1>Página não encontrada</h1>
        <p>Confira o endereço ou volte ao início para continuar.</p>
        <Link className="button" href="/">
          Voltar ao início
        </Link>
      </section>
    </main>
  );
}
