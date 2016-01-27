package lt.msemys.esjc.tcp;

import io.netty.channel.Channel;

import java.io.Serializable;

import static lt.msemys.esjc.util.Preconditions.checkNotNull;

public class ChannelId implements Comparable<CharSequence>, CharSequence, Serializable {

    private final String value;

    private ChannelId(Channel channel) {
        checkNotNull(channel, "channel");
        value = String.format("0x%08x", (int) channel.hashCode());
    }

    @Override
    public int length() {
        return value.length();
    }

    @Override
    public char charAt(int index) {
        return value.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return value.subSequence(start, end);
    }

    @Override
    public int compareTo(CharSequence o) {
        return value.compareTo(o.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChannelId that = (ChannelId) o;

        return value.equals(that.value);

    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }

    public static ChannelId of(Channel channel) {
        return new ChannelId(channel);
    }

}
