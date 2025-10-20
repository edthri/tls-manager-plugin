package org.openintegrationengine.tlsmanager.shared.models;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@XStreamAlias("trustedCertificate")
public class TrustedCertificate implements Serializable {

    public TrustedCertificate(String alias) {
        this.alias = alias;
    }

    private String alias;
    private String certificate;
}
