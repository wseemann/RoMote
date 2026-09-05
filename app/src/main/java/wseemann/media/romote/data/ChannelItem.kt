package wseemann.media.romote.data

data class ChannelItem(val id: String, val title: String, val iconUrl: String) {

    companion object {
        /**
         * The one place the icon url is built. [host] is a full base url with no trailing slash,
         * e.g. http://192.168.1.20:8060, and it changes whenever the device is rediscovered - which
         * is why recents store the channel id and rebuild the url rather than persisting it.
         */
        fun iconUrl(host: String, channelId: String): String {
            return host + "/query/icon/" + channelId
        }
    }
}
