package com.paicbd.smsc.dto.diameter;

import com.paicbd.smsc.utils.Generated;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

@Slf4j
@Getter
@Setter
@Generated
@NoArgsConstructor
@XmlRootElement(name = "Configuration")
@XmlAccessorType(XmlAccessType.FIELD)
public class DiameterXmlConfig {
    @XmlElement(name = "LocalPeer")
    private LocalPeerXml localPeer;

    @XmlElement(name = "Parameters")
    private ParametersXml parameters;

    @XmlElement(name = "Network")
    private NetworkXml network;

    @XmlElement(name = "Extensions")
    private ExtensionsXml extensions;

    @SuppressWarnings("unused")
    @XmlAttribute(name = "xmlns")
    private static String xmlns = "http://www.jdiameter.org/jdiameter-server";

    public DiameterXmlConfig(DiameterConfig config) {
        this.localPeer = new LocalPeerXml(config.getLocalPeer());
        this.parameters = new ParametersXml(config.getParameters());
        this.network = new NetworkXml(config.getNetwork());
        this.extensions = new ExtensionsXml(config.getExtensionMode());
    }

    @Getter
    @Setter
    @Generated
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class LocalPeerXml {
        @XmlElement(name = "URI")
        private XmlValue uri;

        @XmlElementWrapper(name = "IPAddresses")
        @XmlElement(name = "IPAddress")
        private List<XmlValue> ipAddresses;

        @XmlElement(name = "Realm")
        private XmlValue realm;

        @XmlElement(name = "VendorID")
        private XmlValue vendorId;

        @XmlElement(name = "ProductName")
        private XmlValue productName;

        @XmlElement(name = "FirmwareRevision")
        private XmlValue firmwareRevision;

        @XmlElementWrapper(name = "Applications")
        @XmlElement(name = "ApplicationID")
        private List<ApplicationXml> applications;

        public LocalPeerXml(LocalPeer localPeer) {
            this.uri = new XmlValue(localPeer.getUri());
            this.ipAddresses = localPeer.getIpAddresses().stream().map(XmlValue::new).toList();
            this.realm = new XmlValue(localPeer.getRealm());
            this.vendorId = new XmlValue(localPeer.getVendorId());
            this.productName = new XmlValue(localPeer.getProductName());
            this.firmwareRevision = new XmlValue(localPeer.getFirmwareRevision());
            this.applications = localPeer.getApplications().stream().map(ApplicationXml::new).toList();
        }
    }

    @Getter
    @Setter
    @Generated
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ApplicationXml {
        @XmlElement(name = "VendorId")
        private XmlValue vendorId;

        @XmlElement(name = "AuthApplId")
        private XmlValue authApplId;

        @XmlElement(name = "AcctApplId")
        private XmlValue acctApplId;

        public ApplicationXml(Application application) {
            this.vendorId = new XmlValue(application.getVendorId());
            this.authApplId = new XmlValue(application.getAuthApplId());
            this.acctApplId = new XmlValue(application.getAcctApplId());
        }
    }

    @Getter
    @Setter
    @Generated
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ParametersXml {
        @XmlElement(name = "AcceptUndefinedPeer")
        private XmlValue acceptUndefinedPeer;
        @XmlElement(name = "DuplicateProtection")
        private XmlValue duplicateProtection;
        @XmlElement(name = "DuplicateTimer")
        private XmlValue duplicateTimer;
        @XmlElement(name = "DuplicateSize")
        private XmlValue duplicateSize;
        @XmlElement(name = "UseUriAsFqdn")
        private XmlValue useUriAsFqdn;
        @XmlElement(name = "SingleLocalPeer")
        private XmlValue singleLocalPeer;
        @XmlElement(name = "QueueSize")
        private XmlValue queueSize;
        @XmlElement(name = "MessageTimeOut")
        private XmlValue messageTimeOut;
        @XmlElement(name = "SessionTimeOut")
        private XmlValue sessionTimeOut;
        @XmlElement(name = "StopTimeOut")
        private XmlValue stopTimeOut;
        @XmlElement(name = "CeaTimeOut")
        private XmlValue ceaTimeOut;
        @XmlElement(name = "IacTimeOut")
        private XmlValue iacTimeOut;
        @XmlElement(name = "DwaTimeOut")
        private XmlValue dwaTimeOut;
        @XmlElement(name = "DpaTimeOut")
        private XmlValue dpaTimeOut;
        @XmlElement(name = "RecTimeOut")
        private XmlValue recTimeOut;
        @XmlElement(name = "BindDelay")
        private XmlValue bindDelay;
        @XmlElement(name = "PeerFSMThreadCount")
        private XmlValue peerFSMThreadCount;
        @XmlElement(name = "RequestTable")
        private RequestTableXml requestTable;

        public ParametersXml(Parameters parameters) {
            this.acceptUndefinedPeer = new XmlValue(parameters.isAcceptUndefinedPeer());
            this.duplicateProtection = new XmlValue(parameters.isDuplicateProtection());
            this.duplicateTimer = new XmlValue(parameters.getDuplicateTimer());
            this.duplicateSize = new XmlValue(parameters.getDuplicateSize());
            this.useUriAsFqdn = new XmlValue(parameters.isUseUriAsFqdn());
            this.queueSize = new XmlValue(parameters.getQueueSize());
            this.messageTimeOut = new XmlValue(parameters.getMessageTimeOut());
            this.stopTimeOut = new XmlValue(parameters.getStopTimeOut());
            this.ceaTimeOut = new XmlValue(parameters.getCeaTimeOut());
            this.iacTimeOut = new XmlValue(parameters.getIacTimeOut());
            this.dwaTimeOut = new XmlValue(parameters.getDwaTimeOut());
            this.dpaTimeOut = new XmlValue(parameters.getDpaTimeOut());
            this.recTimeOut = new XmlValue(parameters.getRecTimeOut());
            this.peerFSMThreadCount = new XmlValue(parameters.getPeerFSMThreadCount());
            this.sessionTimeOut = new XmlValue(parameters.getSessionTimeOut());
            this.singleLocalPeer = new XmlValue(parameters.isSingleLocalPeer());
            this.bindDelay = new XmlValue(parameters.getBindDelay());
            this.requestTable = new RequestTableXml(parameters.getRequestTable());
        }
    }

    @Getter
    @Setter
    @Generated
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class RequestTableXml {
        @XmlAttribute(name = "size")
        private int size;
        @XmlAttribute(name = "clear_size")
        private int clearSize;

        public RequestTableXml(RequestTable requestTable) {
            this.size = requestTable.getSize();
            this.clearSize = requestTable.getClearSize();
        }
    }

    @Getter
    @Setter
    @Generated
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class NetworkXml {
        @XmlElementWrapper(name = "Peers")
        @XmlElement(name = "Peer")
        private List<PeerXml> peers;

        @XmlElementWrapper(name = "Realms")
        @XmlElement(name = "Realm")
        private List<RealmXml> realms;

        public NetworkXml(Network network) {
            this.peers = network.getPeers().stream().map(PeerXml::new).toList();
            this.realms = network.getRealms().stream().map(RealmXml::new).toList();
        }
    }

    @Getter
    @Setter
    @Generated
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PeerXml {
        @XmlAttribute(name = "name")
        private String name;
        @XmlAttribute(name = "attempt_connect")
        private boolean attemptConnect;
        @XmlAttribute(name = "rating")
        private int rating;
        @XmlAttribute(name = "host")
        private String host;
        @XmlAttribute(name = "applications")
        private String applications;
        @XmlAttribute(name = "ip")
        private String ip;
        @XmlAttribute(name = "portRange")
        private String portRange;
        @XmlAttribute(name = "security_ref")
        private String securityRef;
        @XmlAttribute(name = "standby_addresses")
        private String standbyAddresses;

        public PeerXml(Peer peer) {
            this.name = peer.getUri();
            this.attemptConnect = peer.isAttemptConnect();
            this.rating = peer.getRating();
            this.host = peer.getHost();
            this.applications = peer.getApplications();
            this.ip = peer.getIp();
            this.portRange = peer.getPortRange();
            this.securityRef = peer.getSecurityRef();
            this.standbyAddresses = peer.getStandbyAddresses();
        }
    }

    @Getter
    @Setter
    @Generated
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class RealmXml {
        @XmlAttribute(name = "name")
        private String name;
        @XmlAttribute(name = "peers")
        private String peers;
        @XmlAttribute(name = "local_action")
        private String localAction;
        @XmlAttribute(name = "dynamic")
        private boolean dynamic;
        @XmlAttribute(name = "exp_time")
        private int expTime;
        @XmlElement(name = "ApplicationID")
        private ApplicationXml application;

        public RealmXml(Realm realm) {
            this.name = realm.getUri();
            this.peers = realm.getPeers();
            this.localAction = realm.getLocalAction();
            this.dynamic = realm.isDynamic();
            this.expTime = realm.getExpTime();
            this.application = new ApplicationXml(realm.getApplication());
        }
    }

    @Getter
    @Setter
    @Generated
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ExtensionsXml {
        @XmlElement(name = "Connection")
        private XmlValue connection;
        @XmlElement(name = "NetworkGuard")
        private XmlValue networkGuard;

        public ExtensionsXml(ConnectionType connectionType) {
            this.connection = (Objects.equals(connectionType, ConnectionType.SCTP)) ?
                    new XmlValue("org.jdiameter.client.impl.transport.sctp.SCTPClientConnection") :
                    new XmlValue("org.jdiameter.client.impl.transport.tcp.netty.TCPClientConnection");
            this.networkGuard = (Objects.equals(connectionType, ConnectionType.SCTP)) ?
                    new XmlValue("org.jdiameter.server.impl.io.sctp.NetworkGuard") :
                    new XmlValue("org.jdiameter.server.impl.io.tcp.netty.NetworkGuard");
        }
    }

    @Getter
    @Setter
    @Generated
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class XmlValue {
        @XmlAttribute(name = "value")
        private String value;

        public XmlValue(Object value) {
            this.value = String.valueOf(value);
        }
    }
}

