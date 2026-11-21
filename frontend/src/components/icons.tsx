import type { ReactNode } from 'react'
import { LOGO_INNER, LOGO_STROKE } from '../logo'

/** 인라인 SVG 아이콘. 24x24 viewBox, currentColor 선, 굵기 1.75, 장식용이라 aria-hidden */
function Icon({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75"
         strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
      {children}
    </svg>
  )
}

export const SearchIcon = ({ className }: { className?: string }) => (
  <Icon className={className}><circle cx="11" cy="11" r="6.5" /><path d="m20 20-4.2-4.2" /></Icon>
)

export const CloseIcon = ({ className }: { className?: string }) => (
  <Icon className={className}><path d="M6 6l12 12M18 6 6 18" /></Icon>
)

type IconProps = { className?: string }

export const GridIcon = ({ className }: IconProps) => (
  <Icon className={className}><rect x="4" y="4" width="6.5" height="6.5" rx="1.5" /><rect x="13.5" y="4" width="6.5" height="6.5" rx="1.5" /><rect x="4" y="13.5" width="6.5" height="6.5" rx="1.5" /><rect x="13.5" y="13.5" width="6.5" height="6.5" rx="1.5" /></Icon>
)
export const KeyboardIcon = ({ className }: IconProps) => (
  <Icon className={className}><rect x="2.5" y="6" width="19" height="12" rx="2.5" /><path d="M6.5 10h.01M10 10h.01M14 10h.01M17.5 10h.01M7.5 14h9" /></Icon>
)
export const MouseIcon = ({ className }: IconProps) => (
  <Icon className={className}><rect x="6" y="3" width="12" height="18" rx="6" /><path d="M12 7v4" /></Icon>
)
export const MonitorIcon = ({ className }: IconProps) => (
  <Icon className={className}><rect x="3" y="4" width="18" height="12" rx="2" /><path d="M8 20h8M12 16v4" /></Icon>
)
export const HeadphoneIcon = ({ className }: IconProps) => (
  <Icon className={className}><path d="M4 15v-3a8 8 0 0 1 16 0v3" /><rect x="3" y="14" width="4" height="6.5" rx="1.5" /><rect x="17" y="14" width="4" height="6.5" rx="1.5" /></Icon>
)
export const AccessoryIcon = ({ className }: IconProps) => (
  <Icon className={className}><path d="M9 3v5M15 3v5M7 8h10v3a5 5 0 0 1-10 0V8ZM12 16v5" /></Icon>
)
export const BoxIcon = ({ className }: IconProps) => (
  <Icon className={className}><path d="M21 8 12 3 3 8v8l9 5 9-5V8ZM3 8l9 5 9-5M12 13v8" /></Icon>
)
/** 빈 결과용 돋보기(안에 빼기 표시) */
export const EmptySearchIcon = ({ className }: IconProps) => (
  <Icon className={className}><circle cx="11" cy="11" r="6.5" /><path d="m20 20-4.2-4.2M8.5 11h5" /></Icon>
)

/** 카테고리 코드 → 선 아이콘. 모르는 코드는 상자 */
export function CategoryIcon({ code, className }: { code: string | null | undefined; className?: string }) {
  switch (code) {
    case 'KEYBOARD': return <KeyboardIcon className={className} />
    case 'MOUSE': return <MouseIcon className={className} />
    case 'MONITOR': return <MonitorIcon className={className} />
    case 'AUDIO': return <HeadphoneIcon className={className} />
    case 'ACCESSORY': return <AccessoryIcon className={className} />
    default: return <BoxIcon className={className} />
  }
}

/** 로고: accent 면의 라운드 사각형 + 번개 */
/** 도형은 src/logo.ts 한 곳에서 온다 (정적 문자열이라 안전). 탭 아이콘은 `npm run logo`로 같은 도형에서 만든다 */
export function LogoMark() {
  return (
    <span className="logo-mark">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={LOGO_STROKE} strokeLinecap="round" strokeLinejoin="round"
           aria-hidden="true" focusable="false" dangerouslySetInnerHTML={{ __html: LOGO_INNER }} />
    </span>
  )
}

export const ChevronLeftIcon = ({ className }: { className?: string }) => (
  <Icon className={className}><path d="m15 6-6 6 6 6" /></Icon>
)
export const ChevronRightIcon = ({ className }: { className?: string }) => (
  <Icon className={className}><path d="m9 6 6 6-6 6" /></Icon>
)
export const PauseIcon = ({ className }: { className?: string }) => (
  <Icon className={className}><path d="M9 6.5v11M15 6.5v11" /></Icon>
)
export const PlayIcon = ({ className }: { className?: string }) => (
  <Icon className={className}><path d="M8.5 6v12L18 12 8.5 6Z" /></Icon>
)

export const ChevronDownIcon = ({ className }: { className?: string }) => (
  <Icon className={className}><path d="m6 9 6 6 6-6" /></Icon>
)

/** 채워진 별(평점 표시용). 색은 currentColor, 장식이라 aria-hidden */
export const StarIcon = ({ className }: { className?: string }) => (
  <svg className={className} viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" focusable="false">
    <path d="M12 2.8l2.9 5.9 6.5.9-4.7 4.6 1.1 6.5L12 17.6l-5.8 3.1 1.1-6.5L2.6 9.6l6.5-.9L12 2.8Z" />
  </svg>
)
/** 작은 불꽃(핫딜 배지용) */
export const FlameIcon = ({ className }: { className?: string }) => (
  <svg className={className} viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" focusable="false">
    <path d="M12.5 2c.4 3-1.2 4.6-2.6 6.2C8.5 9.8 7 11.4 7 14a5 5 0 0 0 10 0c0-1.6-.7-2.9-1.5-3.9-.3 1-.9 1.6-1.6 1.9.5-3.4-.2-6.8-1.4-10Z" />
  </svg>
)
/** 인기(오르는 선) */
export const TrendIcon = ({ className }: { className?: string }) => (
  <Icon className={className}><path d="m3 17 6-6 4 4 8-8M15 7h6v6" /></Icon>
)
