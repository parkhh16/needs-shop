import { Link } from 'react-router-dom'
import type { Product } from '../api'
import { LOW_STOCK_THRESHOLD, won } from '../format'
import { FlameIcon, TrendIcon } from './icons'
import { ProductImage } from './ProductImage'
import { RatingLine } from './Rating'

/** 품절이면 배지를 보여준다 (관리자 표용). 남은 시간이나 판매율처럼 구매를 재촉하는 표시는 두지 않는다 */
export function StatusBadge({ product }: { product: Product }) {
  return product.soldOut ? <span className="badge soldout">품절</span> : null
}

export function ProductCard({ product, featured = false }: { product: Product; featured?: boolean }) {
  const low = !product.soldOut && product.stock > 0 && product.stock <= LOW_STOCK_THRESHOLD
  const hot = !product.soldOut && !!product.badges?.includes('HOT_DEAL')
  const popular = !product.soldOut && !!product.badges?.includes('POPULAR')
  return (
    <Link to={`/products/${product.id}`} className={`card${product.soldOut ? ' soldout' : ''}${featured ? ' featured' : ''}`}>
      <div className="card-media">
        <ProductImage src={product.thumbnail} alt={product.name} category={product.category} categoryLabel={product.categoryLabel} eager={featured} />
        {(hot || popular) && (
          <div className="card-badges">
            {hot && <span className="card-badge hot" role="img" aria-label="핫딜: 할인 중이고 수량이 얼마 남지 않았어요" title="핫딜: 할인 중이고 수량이 얼마 남지 않았어요"><FlameIcon /></span>}
            {popular && <span className="card-badge pop"><TrendIcon />인기</span>}
          </div>
        )}
        {low && <span className="card-chip">{product.stock}개 남음</span>}
        {product.soldOut && <div className="card-scrim"><span>품절</span></div>}
      </div>
      <div className="card-body">
        <div className="cat">{product.categoryLabel} · {product.subCategoryLabel}</div>
        <h3>{product.name}</h3>
        <RatingLine average={product.ratingAvg} count={product.reviewCount} />
        <div className="price-row">
          {product.discountRate > 0 && <span className="discount">{product.discountRate}%</span>}
          <span className="price">{won(product.price)}</span>
          {product.originalPrice && product.discountRate > 0 && <span className="original">{won(product.originalPrice)}</span>}
        </div>
        {product.perUserLimit && <div className="limit-note">1인 {product.perUserLimit}개까지</div>}
      </div>
    </Link>
  )
}
