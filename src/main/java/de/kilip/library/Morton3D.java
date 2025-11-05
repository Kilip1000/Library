package de.kilip.library;

import java.math.BigInteger;

//Not used anywhere, but i wanna do x,y,z coords later
public class Morton3D {
    //Just for compactness
    static final BigInteger ONE = BigInteger.ONE;
    static final BigInteger ZERO = BigInteger.ZERO;

    public static BigInteger encode(BigInteger x, BigInteger y, BigInteger z) {
        BigInteger result = ZERO;
        BigInteger mask = ONE;


        int bitIndex = 0;
        while (!x.equals(ZERO) || !y.equals(ZERO) || !z.equals(ZERO)) {
            BigInteger xb = x.and(mask);
            BigInteger yb = y.and(mask);
            BigInteger zb = z.and(mask);

            //interleave bits: x ---> bitIndex+2, y ---> bitIndex+1, z --->  bitIndex
            result = result.or(xb.shiftLeft(bitIndex + 2)).or(yb.shiftLeft(bitIndex + 1)).or(zb.shiftLeft(bitIndex));
            x = x.shiftRight(1);
            y = y.shiftRight(1);
            z = z.shiftRight(1);

            bitIndex += 3;
        }
        return result;
    }


    public static BigInteger[] decode(BigInteger seed) {
        BigInteger mask = ONE;
        BigInteger x = ZERO;
        BigInteger y = ZERO;
        BigInteger z = ZERO;

        int bitIndex = 0;
        while (seed.compareTo(ZERO) > 0) {

            // Extract interleaved bits
            BigInteger zb = seed.and(mask);
            BigInteger yb = seed.shiftRight(1).and(mask);
            BigInteger xb = seed.shiftRight(2).and(mask);

            x = x.or(xb.shiftLeft(bitIndex));
            y = y.or(yb.shiftLeft(bitIndex));
            z = z.or(zb.shiftLeft(bitIndex));

            seed = seed.shiftRight(3);
            bitIndex++;
        }

        return new BigInteger[]{x, y, z};
    }


}
