package com.paicbd.smsc.dto;

import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.Generated;
import com.paicbd.smsc.utils.RegEventState;
import com.paicbd.smsc.utils.RegistrationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Generated
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRegister {
    private String msisdn;
    private String aor;
    private String imsi;
    private RegistrationType registrationType;
    private RegEventState regEventState;
    private String gsmScfAddress;
    private boolean supportSbi = false;

    @Override
    public String toString() {
        return Converter.valueAsString(this);
    }
}
