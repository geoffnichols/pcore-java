package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

import java.util.List;
import java.util.Objects;

import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;

public class NodeDefinition extends Definition {
	public final Expression parent;
	public final List<Expression> hostMatches;
	public final Expression body;

	public NodeDefinition(List<Expression> hostMatches, Expression parent, Expression body, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.hostMatches = unmodifiableCopy(hostMatches);
		this.parent = parent;
		this.body = body;
	}

	public boolean equals(Object o) {
		if(!super.equals(o))
			return false;
		NodeDefinition co = (NodeDefinition)o;
		return hostMatches.equals(co.hostMatches) && Objects.equals(parent, co.parent) && body.equals(co.body);
	}
}
