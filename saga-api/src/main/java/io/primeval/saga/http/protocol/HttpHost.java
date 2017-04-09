package io.primeval.saga.http.protocol;

public final class HttpHost {

    public final String protocol;
    public final String host;
    public final int port;

    public HttpHost(String protocol, String host, int port) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
    }

    public boolean isSecure() {
        return "https".equals(protocol);
    }
    
    
    public String repr() {
        return protocol + "://" + host + ":" + port;
    }
    @Override
    public String toString() {
        return "HttpHost [protocol=" + protocol + ", host=" + host + ", port=" + port + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + port;
        result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        HttpHost other = (HttpHost) obj;
        if (host == null) {
            if (other.host != null) {
                return false;
            }
        } else if (!host.equals(other.host)) {
            return false;
        }
        if (port != other.port) {
            return false;
        }
        if (protocol == null) {
            if (other.protocol != null) {
                return false;
            }
        } else if (!protocol.equals(other.protocol)) {
            return false;
        }
        return true;
    }

}
