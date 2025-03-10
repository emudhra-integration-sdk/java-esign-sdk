package org.emcastle.pqc.crypto.gmss;

import org.emcastle.crypto.params.AsymmetricKeyParameter;

public class GMSSKeyParameters
    extends AsymmetricKeyParameter
{
    private GMSSParameters params;

    public GMSSKeyParameters(
        boolean isPrivate,
        GMSSParameters params)
    {
        super(isPrivate);
        this.params = params;
    }

    public GMSSParameters getParameters()
    {
        return params;
    }
}