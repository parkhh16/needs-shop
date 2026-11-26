import { ApiError } from './api'

const IN_PROGRESS = 'I003'

/**
 * 결과를 모르는 실패인가. 이때는 서버가 이미 처리했을 수 있으므로 같은 Idempotency-Key로 다시 보내야
 * 서버가 같은 요청으로 알아보고 첫 응답을 돌려준다(주문·결제가 두 번 생기지 않음).
 * - 0: 네트워크 오류(응답 유실 포함)
 * - 5xx: 서버 오류나 프록시 타임아웃(502·504). 서버는 처리 중 예외가 나면 멱등 레코드를 지우므로 대부분 같은 키 재시도가 안전하다.
 *   예외: 처리 중 서버가 죽어 레코드가 IN_PROGRESS로 남는 경우(아래 I003 상한으로 빠져나온다), 커밋 뒤 완료 기록이 실패한 경우(드묾, 문서 05 남은 한계)
 * - 409 I003: 같은 키 요청이 아직 처리 중
 * - ApiError가 아닌 예외: 응답 헤더를 받은 뒤 본문을 읽다 끊긴 경우(TypeError) 등
 * 그 밖의 4xx는 서버가 명확히 거절한 것이므로 키를 버린다.
 */
export function isOutcomeUnknown(e: unknown): boolean {
  if (!(e instanceof ApiError)) return true
  return e.status === 0 || e.status >= 500 || (e.status === 409 && e.code === IN_PROGRESS)
}

/**
 * 요청 하나(scope)에 쓸 멱등 키를 보관한다. 결과를 모르는 실패 뒤 같은 요청을 다시 보내면 같은 키를 돌려주고,
 * 성공하거나 명확히 거절되면 버린다. scope는 요청 내용까지 담아야 한다(같은 키로 다른 본문을 보내면 서버가 422로 거절).
 * 키는 화면 메모리에만 있어 페이지를 떠나면 사라진다(그 뒤 재시도는 새 키).
 */
export class IdempotencyKeys {
  private keys = new Map<string, string>()
  private inProgressSeen = new Set<string>()

  get(scope: string): string {
    let key = this.keys.get(scope)
    if (!key) {
      key = crypto.randomUUID()
      this.keys.set(scope, key)
    }
    return key
  }

  /**
   * 요청이 끝난 뒤 호출한다. 결과를 모르는 실패면 키를 남기고, 그 밖에는 버린다.
   * I003이 두 번 이어지면 서버 레코드가 IN_PROGRESS로 남았다고 보고 키를 버린다. 계속 같은 키를 쓰면 새로고침 전까지 막힌다.
   */
  settle(scope: string, error?: unknown) {
    const inProgress = error instanceof ApiError && error.status === 409 && error.code === IN_PROGRESS
    if (error !== undefined && isOutcomeUnknown(error) && !(inProgress && this.inProgressSeen.has(scope))) {
      if (inProgress) this.inProgressSeen.add(scope)
      else this.inProgressSeen.delete(scope)
      return
    }
    this.keys.delete(scope)
    this.inProgressSeen.delete(scope)
  }
}
