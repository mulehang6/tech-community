package com.github.paicoding.forum.core.cache;

import com.github.paicoding.forum.core.util.JsonUtil;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.util.CollectionUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author YiHui
 * 创建于 2023/2/7
 */
public class RedisClient {
    private static final Charset CODE = StandardCharsets.UTF_8;
    private static final String KEY_PREFIX = "pai_";
    private static RedisTemplate<String, String> template;

    public static void register(RedisTemplate<String, String> template) {
        RedisClient.template = template;
    }

    public static void nullCheck(Object... args) {
        for (Object obj : args) {
            if (obj == null) {
                throw new IllegalArgumentException("redis argument can not be null!");
            }
        }
    }

    /**
     * 技术派的缓存值序列化处理
     */
    public static <T> byte[] valBytes(T val) {

        if (val instanceof String) {
            // 将 val 强制转换为String
            return ((String) val).getBytes(CODE);
        } else {
            return JsonUtil.toStr(val).getBytes(CODE);
        }
    }

    /**
     * 生成技术派的缓存key
     */
    public static byte[] keyBytes(String key) {
        nullCheck(key);
        key = KEY_PREFIX + key;
        return key.getBytes(CODE);
    }

    public static byte[][] keyBytes(List<String> keys) {
        byte[][] bytes = new byte[keys.size()][];
        int index = 0;
        for (String key : keys) {
            bytes[index++] = keyBytes(key);
        }
        return bytes;
    }

    /**
     * 返回key的有效期
     */
    public static Long ttl(String key) {
        return template.execute((RedisCallback<Long>) con -> con.ttl(keyBytes(key)));
    }

    /**
     * 查询缓存
     */
    public static String getStr(String key) {
        return template.execute((RedisCallback<String>) con -> {
            byte[] val = con.get(keyBytes(key));
            return val == null ? null : new String(val, CODE);
        });
    }

    /**
     * 设置缓存
     */
    public static void setStr(String key, String value) {
        template.execute((RedisCallback<Void>) con -> {
            con.set(keyBytes(key), valBytes(value));
            return null;
        });
    }

    /**
     * 删除缓存
     */
    public static void del(String key) {
        template.execute((RedisCallback<Long>) con -> con.del(keyBytes(key)));
    }

    /**
     * 设置缓存有效期
     *
     * @param expire 有效期，s为单位
     */
    public static void expire(String key, Long expire) {
        template.execute((RedisCallback<Void>) connection -> {
            connection.expire(keyBytes(key), expire);
            return null;
        });
    }

    /**
     * 带过期时间的缓存写入
     *
     * @param expire s为单位
     */
    public static Boolean setStrWithExpire(String key, String value, Long expire) {
        return template.execute((RedisCallback<Boolean>) redisConnection -> redisConnection.setEx(keyBytes(key), expire, valBytes(value)));
    }

    /**
     * 仅当key不存在时写入缓存，并设置过期时间
     *
     * @param expire s为单位
     * @return true 写入成功；false key已存在
     */
    public static Boolean setStrIfAbsentWithExpire(String key, String value, Long expire) {
        return template.execute((RedisCallback<Boolean>) redisConnection -> redisConnection.set(keyBytes(key), valBytes(value), Expiration.seconds(expire), RedisStringCommands.SetOption.SET_IF_ABSENT));
    }

    public static <T> Map<String, T> hGetAll(String key, Class<T> clz) {
        Map<byte[], byte[]> records = template.execute((RedisCallback<Map<byte[], byte[]>>) con -> con.hGetAll(keyBytes(key)));
        if (records == null) {
            return Collections.emptyMap();
        }

        Map<String, T> result = Maps.newHashMapWithExpectedSize(records.size());
        for (Map.Entry<byte[], byte[]> entry : records.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }

            result.put(new String(entry.getKey(), CODE), toObj(entry.getValue(), clz));
        }
        return result;
    }

    public static <T> T hGet(String key, String field, Class<T> clz) {
        return template.execute((RedisCallback<T>) con -> {
            byte[] records = con.hGet(keyBytes(key), valBytes(field));
            if (records == null) {
                return null;
            }

            return toObj(records, clz);
        });
    }

    /**
     * 自增
     */
    public static Long hIncr(String key, String filed, Integer cnt) {
        return template.execute((RedisCallback<Long>) con -> con.hIncrBy(keyBytes(key), valBytes(filed), cnt));
    }

    public static Boolean hDel(String key, String field) {
        return template.execute((RedisCallback<Boolean>) connection -> {
            Long count = connection.hDel(keyBytes(key), valBytes(field));
            return count != null && count > 0;
        });
    }

    public static <T> Boolean hSet(String key, String field, T ans) {
        return template.execute((RedisCallback<Boolean>) redisConnection -> redisConnection.hSet(keyBytes(key), valBytes(field), valBytes(ans)));
    }

    public static <T> void hMSet(String key, Map<String, T> fields) {
        Map<byte[], byte[]> val = Maps.newHashMapWithExpectedSize(fields.size());
        for (Map.Entry<String, T> entry : fields.entrySet()) {
            val.put(valBytes(entry.getKey()), valBytes(entry.getValue()));
        }
        template.execute((RedisCallback<Object>) connection -> {
            connection.hMSet(keyBytes(key), val);
            return null;
        });
    }

    public static <T> Map<String, T> hMGet(String key, final List<String> fields, Class<T> clz) {
        return template.execute((RedisCallback<Map<String, T>>) connection -> {
            byte[][] f = new byte[fields.size()][];
            IntStream.range(0, fields.size()).forEach(i -> f[i] = valBytes(fields.get(i)));
            List<byte[]> ans = connection.hMGet(keyBytes(key), f);
            if (ans == null) {
                return Collections.emptyMap();
            }

            Map<String, T> result = Maps.newHashMapWithExpectedSize(fields.size());
            IntStream.range(0, Math.min(fields.size(), ans.size()))
                    .forEach(i -> result.put(fields.get(i), toObj(ans.get(i), clz)));
            return result;
        });
    }

    /**
     * 判断value是否再set中
     */
    public static <T> Boolean sIsMember(String key, T value) {
        return template.execute((RedisCallback<Boolean>) connection -> connection.sIsMember(keyBytes(key), valBytes(value)));
    }

    /**
     * 获取set中的所有内容
     */
    public static <T> Set<T> sGetAll(String key, Class<T> clz) {
        return template.execute((RedisCallback<Set<T>>) connection -> {
            Set<byte[]> set = connection.sMembers(keyBytes(key));
            if (CollectionUtils.isEmpty(set)) {
                return Collections.emptySet();
            }
            return set.stream().map(s -> toObj(s, clz)).collect(Collectors.toSet());
        });
    }

    /**
     * 往set中添加内容
     */
    public static <T> boolean sPut(String key, T val) {
        Long count = template.execute((RedisCallback<Long>) connection -> connection.sAdd(keyBytes(key), valBytes(val)));
        return count != null && count > 0;
    }

    /**
     * 移除set中的内容
     */
    public static <T> void sDel(String key, T val) {
        template.execute((RedisCallback<Void>) connection -> {
            connection.sRem(keyBytes(key), valBytes(val));
            return null;
        });
    }


    /**
     * 分数更新
     */
    public static Double zIncrBy(String key, String value, Integer score) {
        return template.execute((RedisCallback<Double>) connection -> connection.zIncrBy(keyBytes(key), score, valBytes(value)));
    }

    public static ImmutablePair<Integer, Double> zRankInfo(String key, String value) {
        double score = zScore(key, value);
        int rank = zRank(key, value);
        return ImmutablePair.of(rank, score);
    }

    /**
     * 获取分数
     */
    public static Double zScore(String key, String value) {
        return template.execute((RedisCallback<Double>) connection -> connection.zScore(keyBytes(key), valBytes(value)));
    }

    public static Integer zRank(String key, String value) {
        return template.execute((RedisCallback<Integer>) connection -> {
            Long rank = connection.zRank(keyBytes(key), valBytes(value));
            return rank == null ? null : rank.intValue();
        });
    }

    /**
     * 找出排名靠前的n个
     */
    public static List<ImmutablePair<String, Double>> zTopNScore(String key, int n) {
        return template.execute((RedisCallback<List<ImmutablePair<String, Double>>>) connection -> {
            Set<RedisZSetCommands.Tuple> set = connection.zRangeWithScores(keyBytes(key), -n, -1);
            if (set == null) {
                return Collections.emptyList();
            }
            return set.stream()
                    .map(tuple -> ImmutablePair.of(toObj(tuple.getValue(), String.class), tuple.getScore()))
                    .sorted((o1, o2) -> Double.compare(o2.getRight(), o1.getRight())).collect(Collectors.toList());
        });
    }


    public static <T> Long lPush(String key, T val) {
        return template.execute((RedisCallback<Long>) connection -> connection.lPush(keyBytes(key), valBytes(val)));
    }

    public static <T> Long rPush(String key, T val) {
        return template.execute((RedisCallback<Long>) connection -> connection.rPush(keyBytes(key), valBytes(val)));
    }

    public static <T> List<T> lRange(String key, int start, int size, Class<T> clz) {
        return template.execute((RedisCallback<List<T>>) connection -> {
            List<byte[]> list = connection.lRange(keyBytes(key), start, size);
            if (CollectionUtils.isEmpty(list)) {
                return new ArrayList<>();
            }
            return list.stream().map(k -> toObj(k, clz)).collect(Collectors.toList());
        });
    }

    public static void lTrim(String key, int start, int size) {
        template.execute((RedisCallback<Void>) connection -> {
            connection.lTrim(keyBytes(key), start, size);
            return null;
        });
    }

    private static <T> T toObj(byte[] ans, Class<T> clz) {
        if (ans == null) {
            return null;
        }

        if (clz == String.class) {
            return clz.cast(new String(ans, CODE));
        }

        return JsonUtil.toObj(new String(ans, CODE), clz);
    }


    public static PipelineAction pipelineAction() {
        return new PipelineAction();
    }

    /**
     * redis 管道执行的封装链路
     */
    public static class PipelineAction {
        private final List<Runnable> run = new ArrayList<>();

        private RedisConnection connection;

        public PipelineAction add(String key, BiConsumer<RedisConnection, byte[]> conn) {
            run.add(() -> conn.accept(connection, RedisClient.keyBytes(key)));
            return this;
        }

        public PipelineAction add(String key, String field, ThreeConsumer<RedisConnection, byte[], byte[]> conn) {
            run.add(() -> conn.accept(connection, RedisClient.keyBytes(key), valBytes(field)));
            return this;
        }

        public void execute() {
            template.executePipelined((RedisCallback<Object>) connection -> {
                PipelineAction.this.connection = connection;
                run.forEach(Runnable::run);
                return null;
            });
        }
    }

    @FunctionalInterface
    public interface ThreeConsumer<T, U, P> {
        void accept(T t, U u, P p);
    }
}
