package daikon;

import java.util.Collection;

public class Quant {
    public static class fuzzy
    {
        public static boolean eq(double o1, double o2)
        {
            return Double.compare(o1,o2)==0;
        }
        public static boolean ne(double o1, double o2)
        {
            return Double.compare(o1,o2)!=0;
        }
        public static boolean lt(double o1, double o2)
        {
            return Double.compare(o1,o2)<0;
        }
        public static boolean lte(double o1, double o2)
        {
            return Double.compare(o1,o2)<=0;
        }
        public static boolean gt(double o1, double o2)
        {
            return Double.compare(o1, o2) > 0;
        }
        public static boolean gte(double o1, double o2)
        {
            return Double.compare(o1,o2)>=0;
        }
    }

    public static int size(Object o) {
        if (o == null) {
            return Integer.MAX_VALUE; // return default value
        }
        java.lang.Class<?> c = o.getClass();
        if (c.isArray()) {
            return java.lang.reflect.Array.getLength(o);
        } else if (o instanceof Collection<?>) {
            return ((Collection<?>)o).size();
        } else {
            return Integer.MAX_VALUE; // return default value
        }
    }

    public static boolean sameLength(Object [] seq1, Object [] seq2) {
        return ((seq1 != null)
                && (seq2 != null)
                && seq1.length == seq2.length);
    }

    public static boolean isReverse(Object[] seq1, Object[] seq2) {
        if (seq1.length != seq2.length) {
            return false;
        } else {
            for(int i = 0; i < seq1.length; ++i) {
                if (seq1[i] != seq2[seq2.length - 1 - i]) {
                    return false;
                }
            }

            return true;
        }
    }

    private static boolean eq(Object x, Object y) {
        return (x == y);
    }

    private static boolean ne(Object x, Object y) {
        return x != y;
    }

    public static boolean pairwiseEqual(Object [] seq1, Object [] seq2) {
        if (!sameLength(seq1, seq2)) {
            return false;
        }
        assert seq1 != null && seq2 != null; // because sameLength() = true
        for (int i = 0 ; i < seq1.length ; i++) {
            if (ne(seq1[i], seq2[i])) {
                return false;
            }
        }
        return true;
    }

    public static Object getElement_Object(Object o, long i) {
        if (o == null) {
            return null; // return default value
        }
        java.lang.Class<?> c = o.getClass();
        if (c.isArray()) {
            return java.lang.reflect.Array.get(o, (int)i);
        } else if (o instanceof java.util.AbstractCollection<?>) {
            return java.lang.reflect.Array.get(((java.util.AbstractCollection<?>)o).toArray(), (int)i);
        } else {
            return null; // return default value
        }
    }

    public static boolean eltsNotEqual(Object [] arr, Object elt) {
        if (arr == null) {
            return false;
        }
        for (int i = 0 ; i < arr.length ; i++) {
            if (eq(arr[i], elt)) {
                return false;
            }
        }
        return true;
    }

    public static boolean eltsEqual(Object [] arr, Object elt) {
        if (arr == null) {
            return false;
        }
        for (int i = 0 ; i < arr.length ; i++) {
            if (!eq(arr[i], elt)) {
                return false;
            }
        }
        return true;
    }

    public static boolean memberOf(Object elt, Object [] arr) {
        if (arr == null) {
            return false;
        }
        for (int i = 0 ; i < arr.length ; i++) {
            if (eq(arr[i], elt)) {
                return true;
            }
        }
        return false;
    }

    public static boolean subsetOf(Object [] seq1, Object [] seq2) {
        if (seq1 == null) {
            return false;
        }
        if (seq2 == null) {
            return false;
        }
        for (int i = 0 ; i < seq1.length ; i++) {
            if (!memberOf(seq1[i], seq2)) {
                return false;
            }
        }
        return true;
    }
}
