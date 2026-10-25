import { Link } from 'react-router-dom'

function Logo() {
  return (
    <Link to="/" className="flex items-center gap-2 font-semibold">
      <img src="/favicon.svg" alt="" className="size-6" />
      Carga UY
    </Link>
  )
}

export default Logo
