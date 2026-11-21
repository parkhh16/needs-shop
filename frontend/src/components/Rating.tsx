import { StarIcon } from './icons'

const FIVE = [0, 1, 2, 3, 4]

/** 소수 별점을 별 5개로 그린다 (장식. 의미는 옆 텍스트나 부모의 aria-label이 전달한다) */
export function Stars({ value, className = '' }: { value: number; className?: string }) {
  const pct = (Math.max(0, Math.min(5, value)) / 5) * 100
  return (
    <span className={`stars ${className}`} aria-hidden="true">
      <span className="stars-base">{FIVE.map(i => <StarIcon key={i} />)}</span>
      <span className="stars-fill" style={{ width: `${pct}%` }}>{FIVE.map(i => <StarIcon key={i} />)}</span>
    </span>
  )
}

/** 카드용 한 줄. 후기가 없으면 muted 문구, 서버가 필드를 안 주면(undefined) 아무것도 그리지 않는다 */
export function RatingLine({ average, count }: { average?: number; count?: number }) {
  if (count === undefined) return null
  if (count <= 0) return <div className="card-rating none">후기 없음</div>
  const avg = (average ?? 0).toFixed(1)
  return (
    <div className="card-rating" role="img" aria-label={`평점 5점 만점에 ${avg}, 후기 ${count}개`}>
      <StarIcon className="card-star" />
      <span className="card-rating-avg">{avg}</span>
      <span className="card-rating-count">({count})</span>
    </div>
  )
}
