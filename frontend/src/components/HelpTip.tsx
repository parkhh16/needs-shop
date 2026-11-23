import { useEffect, useId, useRef, useState, type ReactNode } from 'react'

/**
 * "?" 도움말. 마우스를 올리거나, 키보드로 초점을 주거나, 터치로 누르면 설명이 뜬다.
 * 누르면 고정되고 Esc, 바깥 클릭, 스크롤로 닫힌다.
 * 설명 상자는 position: fixed라서 표(overflow: auto) 안에 있어도 잘리지 않는다.
 * 브라우저 기본 title 속성은 터치·키보드에서 안 보이고 뜨는 데도 느려서 쓰지 않는다.
 */
export function HelpTip({ label, children }: { label: string; children: ReactNode }) {
  const id = useId()
  const wrap = useRef<HTMLSpanElement>(null)
  const btn = useRef<HTMLButtonElement>(null)
  const pinned = useRef(false)
  const [pos, setPos] = useState<{ top: number; left: number } | null>(null)
  const open = pos !== null

  const place = () => {
    const r = btn.current?.getBoundingClientRect()
    if (!r) return
    const width = Math.min(300, window.innerWidth - 16)
    setPos({ top: r.bottom + 6, left: Math.min(Math.max(8, r.left - 8), window.innerWidth - width - 8) })
  }
  const close = () => { pinned.current = false; setPos(null) }

  useEffect(() => {
    if (!open) return
    const onDown = (e: Event) => { if (!wrap.current?.contains(e.target as Node)) close() }
    const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') close() }
    document.addEventListener('pointerdown', onDown)
    document.addEventListener('keydown', onKey)
    window.addEventListener('scroll', close, true)
    return () => {
      document.removeEventListener('pointerdown', onDown)
      document.removeEventListener('keydown', onKey)
      window.removeEventListener('scroll', close, true)
    }
  }, [open])

  return (
    <span className="help-tip" ref={wrap}>
      <button type="button" ref={btn} className="help-btn" aria-label={`${label} 도움말`} aria-describedby={id} aria-expanded={open}
              onMouseEnter={place} onMouseLeave={() => { if (!pinned.current) setPos(null) }}
              onFocus={place} onBlur={close}
              onClick={() => { if (pinned.current) close(); else { pinned.current = true; place() } }}>
        ?
      </button>
      <span role="tooltip" id={id} className="help-pop" hidden={!open} style={pos ?? undefined}>{children}</span>
    </span>
  )
}
