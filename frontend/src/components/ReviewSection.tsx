import { useEffect, useRef, useState } from 'react'
import { api, type Review, type ReviewSummary } from '../api'
import { Stars } from './Rating'

const SIZE = 10
const SCORES = ['5', '4', '3', '2', '1'] as const

/**
 * 상품 상세의 후기 섹션. 후기 목록은 부가 기능이라 불러오기에 실패해도 이 섹션 안에서만 안내하고,
 * 상품 정보와 구매 흐름에는 영향을 주지 않는다 (토스트·전역 오류 처리를 쓰지 않는다).
 */
export function ReviewSection({ productId, summary }: { productId: number; summary: ReviewSummary }) {
  const [items, setItems] = useState<Review[]>([])
  const [page, setPage] = useState(-1) // 마지막으로 받은 페이지 (-1 = 아직 없음)
  const [hasNext, setHasNext] = useState(false)
  const [loading, setLoading] = useState(false)
  const [failed, setFailed] = useState(false)
  const token = useRef(0) // 상품이 바뀌었을 때 늦게 온 응답을 버린다

  const loadPage = (next: number) => {
    const mine = token.current
    setLoading(true)
    setFailed(false)
    api.reviews(productId, next, SIZE)
      .then(r => {
        if (mine !== token.current) return
        setItems(prev => (next === 0 ? r.data.items : [...prev, ...r.data.items]))
        setPage(next)
        setHasNext(r.data.hasNext)
      })
      .catch(() => { if (mine === token.current) setFailed(true) })
      .finally(() => { if (mine === token.current) setLoading(false) })
  }

  useEffect(() => {
    token.current += 1
    setItems([]); setPage(-1); setHasNext(false); setFailed(false); setLoading(false)
    if (summary.count > 0) loadPage(0)
    return () => { token.current += 1 }
    // 상품이 바뀔 때만 다시 받는다 (요약은 같은 상품의 재조회로도 바뀌므로 의존성에서 뺀다)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [productId])

  const total = SCORES.reduce((sum, s) => sum + (summary.distribution[s] ?? 0), 0)
  const avg = summary.average.toFixed(1)

  return (
    <section className="detail-section reviews" aria-labelledby="reviews-title">
      <h2 id="reviews-title">후기 <span className="rv-title-count">{summary.count}</span></h2>

      {summary.count <= 0 ? (
        <p className="rv-empty">아직 후기가 없어요</p>
      ) : (
        <>
          <div className="rv-summary">
            <div className="rv-score">
              <strong className="rv-avg" aria-label={`평점 5점 만점에 ${avg}`}>{avg}</strong>
              <Stars value={summary.average} className="rv-score-stars" />
              <span className="rv-count">후기 {summary.count}개</span>
            </div>
            <ul className="rv-dist" aria-label="점수별 후기 수">
              {SCORES.map(s => {
                const n = summary.distribution[s] ?? 0
                const pct = total > 0 ? (n / total) * 100 : 0
                return (
                  <li key={s}>
                    <span className="rv-dist-label">{s}점</span>
                    <span className="rv-bar" aria-hidden="true"><i style={{ width: `${pct}%` }} /></span>
                    <span className="rv-dist-n">{n}개</span>
                  </li>
                )
              })}
            </ul>
          </div>

          <ul className="rv-list">
            {items.map((r, i) => (
              <li key={`${page}-${i}-${r.nickname}-${r.date}`} className="rv-item">
                <div className="rv-head">
                  <span role="img" aria-label={`5점 만점에 ${r.rating}점`}><Stars value={r.rating} /></span>
                  <span className="rv-nick">{r.nickname}</span>
                  <time className="rv-date" dateTime={r.date}>{r.date}</time>
                </div>
                {r.body && <p className="rv-body">{r.body}</p>}
              </li>
            ))}
          </ul>

          {failed && (
            <div className="rv-error" role="status">
              <span>후기를 불러오지 못했어요. 상품 구매에는 영향이 없어요.</span>
              <button type="button" className="ghost small" onClick={() => loadPage(Math.max(page + 1, 0))}>다시 시도</button>
            </div>
          )}
          {loading && items.length === 0 && <p className="rv-loading" role="status">후기를 불러오는 중…</p>}
          {hasNext && !failed && (
            <button type="button" className="ghost rv-more" onClick={() => loadPage(page + 1)} disabled={loading} aria-busy={loading}>
              {loading ? '불러오는 중…' : '후기 더보기'}
            </button>
          )}
        </>
      )}
      <p className="rv-note">이 후기는 데모용 샘플 데이터예요.</p>
    </section>
  )
}
