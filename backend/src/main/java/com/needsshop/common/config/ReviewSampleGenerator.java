package com.needsshop.common.config;

import com.needsshop.common.config.ReviewTexts.Pool;
import com.needsshop.common.config.ReviewTexts.Spec;
import com.needsshop.product.SubCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 시드 후기(샘플 데이터)를 만든다. 상품 이름으로 정한 고정 난수만 쓰므로 같은 상품은 언제나 같은 후기가 나온다
 * (실행 시각, DB, 상품 id와 무관). 작성 시각은 "몇 분 전"(minutesAgo)으로만 정하고 기준 시각은 호출하는 쪽이 준다.
 *
 * <p>별점 분포는 J자형이다: 5점이 가장 많고 4점이 그다음이며, 1점이 2점보다 많다(양극화).
 * 목표 평균을 주면 1·2·3점 비율을 평균에 따라 정하고, 남는 비율을 4점과 5점이 평균에 맞게 나눠 가진다.
 * 개수는 비율대로 반올림해서 합이 정확히 후기 수가 되게 나눈다 (후기가 적어도 평균이 목표에 가깝다).
 */
final class ReviewSampleGenerator {

    private ReviewSampleGenerator() {
    }

    /** @param minutesAgo 기준 시각에서 몇 분 전에 쓴 후기인지 (최근 6개월 안) */
    record Sample(int rating, String body, String nickname, int minutesAgo) {
    }

    private static final int SIX_MONTHS_MINUTES = 180 * 24 * 60;

    /** 난수와, 한 상품 안에서 문장을 몇 번 썼는지. 덜 쓴 문장을 먼저 골라서 같은 말이 반복되지 않게 한다 */
    private static final class Ctx {
        final Random rng;
        final Map<String, Integer> uses = new HashMap<>();

        Ctx(Random rng) {
            this.rng = rng;
        }

        /** 풀에서 가장 적게 쓴 문장들 중 하나 */
        String pick(String[] pool) {
            int min = Integer.MAX_VALUE;
            for (String s : pool) min = Math.min(min, uses.getOrDefault(s, 0));
            List<String> least = new ArrayList<>();
            for (String s : pool) if (uses.getOrDefault(s, 0) == min) least.add(s);
            String chosen = least.get(rng.nextInt(least.size()));
            uses.merge(chosen, 1, Integer::sum);
            return chosen;
        }
    }

    /** 후기를 만든다. 후기 대상이 아닌 상품(맵에 없음)은 빈 목록 */
    static List<Sample> generate(String productName, SubCategory subCategory) {
        Spec spec = ReviewTexts.PRODUCTS.get(productName);
        if (spec == null) return List.of();
        Pool pool = ReviewTexts.SUBS.get(subCategory);
        Random rng = new Random(seedOf(productName));
        Ctx ctx = new Ctx(rng);

        List<Integer> ratings = ratings(spec, rng);
        int n = ratings.size();

        // 본문 없이 별점만 남긴 후기: 전체의 17~23%, 4·5점에서 고른다
        List<Integer> highIdx = new ArrayList<>();
        for (int i = 0; i < n; i++) if (ratings.get(i) >= 4) highIdx.add(i);
        Collections.shuffle(highIdx, rng);
        int bodyless = (int) Math.round(n * (0.17 + rng.nextDouble() * 0.06));
        Set<Integer> noBody = new HashSet<>(highIdx.subList(0, Math.min(bodyless, highIdx.size())));

        Set<String> usedBodies = new HashSet<>();
        List<Sample> samples = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int rating = ratings.get(i);
            String body = noBody.contains(i) ? null : uniqueBody(rating, spec, pool, ctx, usedBodies);
            int minutesAgo = 120 + (int) (Math.pow(rng.nextDouble(), 1.3) * (SIX_MONTHS_MINUTES - 120));
            samples.add(new Sample(rating, body, nickname(rng), minutesAgo));
        }
        return samples;
    }

    /** 상품 이름으로 고정한 시드. String.hashCode는 JVM이 바뀌어도 같은 값이 나온다 */
    private static long seedOf(String productName) {
        return productName.hashCode() * 31L + 20_261_121L;
    }

    // ── 별점 ──

    private static List<Integer> ratings(Spec spec, Random rng) {
        int n = spec.count();
        List<Integer> list = new ArrayList<>(n);
        if (spec.allFive()) {
            for (int i = 0; i < n; i++) list.add(5);
            return list;
        }
        double[] share = shares(spec.mean());
        int[] counts = new int[5];
        double[] remainder = new double[5];
        int assigned = 0;
        for (int k = 0; k < 5; k++) {
            double exact = share[k] * n;
            counts[k] = (int) Math.floor(exact);
            remainder[k] = exact - counts[k];
            assigned += counts[k];
        }
        // 남은 자리는 소수 부분이 큰 별점부터 하나씩 (같으면 높은 별점 먼저)
        for (int left = n - assigned; left > 0; left--) {
            int best = 0;
            for (int k = 1; k < 5; k++) if (remainder[k] >= remainder[best]) best = k;
            counts[best]++;
            remainder[best] = -1;
        }
        for (int k = 0; k < 5; k++) for (int c = 0; c < counts[k]; c++) list.add(k + 1);
        Collections.shuffle(list, rng);
        return list;
    }

    /** 목표 평균에 맞는 1~5점 비율. 평균이 낮을수록 1~3점이 늘고, 높을수록 줄지만 1점은 최소 3%는 남는다 */
    static double[] shares(double mean) {
        double p1 = clamp(0.035 + (4.7 - mean) * 0.05, 0.03, 0.09);
        double p2 = 0.45 * p1;
        double p3 = clamp(0.8 * p1 + (4.5 - mean) * 0.05, 0.01, 0.14);
        double p5 = mean - 4 + 3 * p1 + 2 * p2 + p3;   // 평균 = 4 - 3p1 - 2p2 - p3 + p5
        double p4 = 1 - p1 - p2 - p3 - p5;
        if (p4 < 0.02) {                                // 평균이 너무 높아 4점이 없어지면 5점에서 덜어 준다
            p5 -= 0.02 - p4;
            p4 = 0.02;
        }
        return new double[]{p1, p2, p3, p4, p5};
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ── 본문 ──

    private static String uniqueBody(int rating, Spec spec, Pool pool, Ctx ctx, Set<String> used) {
        for (int attempt = 0; attempt < 40; attempt++) {
            String body = body(rating, spec, pool, ctx);
            if (used.add(key(body))) return body;
        }
        // 풀이 모자란 아주 드문 경우: 일반 꼬리말을 덧붙여 구분한다
        for (String tail : ReviewTexts.GENERIC_TAIL) {
            String body = body(rating, spec, pool, ctx) + " " + tail;
            if (used.add(key(body))) return body;
        }
        throw new IllegalStateException("후기 문구가 모자랍니다. rating=" + rating);
    }

    /** 같은 문장을 접속어("다만", "근데")나 문장부호만 바꿔 되풀이한 것도 중복으로 본다 */
    private static String key(String body) {
        return body.replaceAll("(다만|근데) ", "").replaceAll("[ .!?]", "");
    }

    private static String body(int rating, Spec spec, Pool pool, Ctx ctx) {
        return switch (rating) {
            case 5 -> five(spec, pool, ctx);
            case 4 -> four(spec, pool, ctx);
            case 3 -> three(pool, ctx);
            case 2 -> ctx.pick(ReviewTexts.TWO_STAR_LEAD) + (ctx.rng.nextBoolean() ? complaint(spec, pool, ctx) : negative(spec, pool, ctx));
            default -> negative(spec, pool, ctx);
        };
    }

    /** 5점: 스펙·사용감 문장 하나(가끔 둘) + 가끔 일반 꼬리말 */
    private static String five(Spec spec, Pool pool, Ctx ctx) {
        Random rng = ctx.rng;
        String first = positive(spec, pool, ctx, true);
        String text;
        if (rng.nextInt(100) < 25) {
            String second = positive(spec, pool, ctx, true);
            text = second.equals(first) ? first : sentence(first, true) + " " + second;
        } else {
            text = first;
        }
        if (rng.nextInt(100) < 35) {
            return sentence(text, true) + " " + ctx.pick(ReviewTexts.GENERIC_TAIL);
        }
        return sentence(text, rng.nextBoolean());
    }

    /** 4점: 좋은 점 + 작은 불만 */
    private static String four(Spec spec, Pool pool, Ctx ctx) {
        String good = positive(spec, pool, ctx, false);
        String connector = ReviewTexts.CONNECTORS[ctx.rng.nextInt(ReviewTexts.CONNECTORS.length)];
        return sentence(good, true) + " " + connector + complaint(spec, pool, ctx);
    }

    private static String three(Pool pool, Ctx ctx) {
        String base = ctx.rng.nextInt(100) < 45 ? ctx.pick(pool.neutral()) : ctx.pick(ReviewTexts.GENERIC_NEUTRAL);
        if (ctx.rng.nextInt(100) < 30) base = base + " " + ctx.pick(ReviewTexts.GENERIC_COMPLAINT);
        return base;
    }

    private static String negative(Spec spec, Pool pool, Ctx ctx) {
        int r = ctx.rng.nextInt(100);
        if (r < 25 && spec.negative().length > 0) return ctx.pick(spec.negative());
        if (r < 60) return ctx.pick(pool.negative());
        return ctx.pick(ReviewTexts.GENERIC_NEGATIVE);
    }

    /** 상품 스펙 문장 > 소분류 문장 > 일반 문장 순으로 자주 뽑는다. 일반 문장은 5점 후기에서만 단독으로 쓴다 */
    private static String positive(Spec spec, Pool pool, Ctx ctx, boolean allowGeneric) {
        int r = ctx.rng.nextInt(100);
        if (r < 55 || (!allowGeneric && r < 80)) return ctx.pick(spec.positive());
        if (r < 80 || !allowGeneric) return ctx.pick(pool.positive());
        return ctx.pick(ReviewTexts.GENERIC_POSITIVE);
    }

    private static String complaint(Spec spec, Pool pool, Ctx ctx) {
        int r = ctx.rng.nextInt(100);
        if (r < 45 && spec.complaint().length > 0) return ctx.pick(spec.complaint());
        if (r < 75) return ctx.pick(pool.complaint());
        return ctx.pick(ReviewTexts.GENERIC_COMPLAINT);
    }

    /** 문장 끝에 마침표를 붙인다 (이미 문장부호가 있으면 그대로). 짧은 한 줄 후기는 마침표 없이도 쓴다 */
    private static String sentence(String text, boolean period) {
        if (!period) return text;
        char last = text.charAt(text.length() - 1);
        return last == '.' || last == '!' || last == '?' ? text : text + ".";
    }

    // ── 닉네임 ──

    private static String nickname(Random rng) {
        int r = rng.nextInt(100);
        if (r < 40) {
            return "구매자 " + letters(rng, 2) + (10 + rng.nextInt(90));
        }
        if (r < 70) {
            return letters(rng, 2 + rng.nextInt(2)) + "***";
        }
        String base = ReviewTexts.NICKNAMES[rng.nextInt(ReviewTexts.NICKNAMES.length)];
        return rng.nextInt(100) < 25 ? base + (1 + rng.nextInt(99)) : base;
    }

    private static String letters(Random rng, int length) {
        char[] c = new char[length];
        for (int i = 0; i < length; i++) c[i] = (char) ('a' + rng.nextInt(26));
        return new String(c);
    }

    /** 테스트와 보고용: 별점별 개수 (1점부터) */
    static int[] distribution(List<Sample> samples) {
        int[] d = new int[5];
        samples.forEach(s -> d[s.rating() - 1]++);
        return d;
    }
}
