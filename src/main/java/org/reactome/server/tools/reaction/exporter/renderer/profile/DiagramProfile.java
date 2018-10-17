package org.reactome.server.tools.reaction.exporter.renderer.profile;

public class DiagramProfile extends ColorProfile {

	private PropertiesColorProfile properties;
	private NodeColorProfile attachment;
	private NodeColorProfile chemical;
	private NodeColorProfile chemicalDrug;
	private NodeColorProfile compartment;
	private NodeColorProfile complex;
	private NodeColorProfile entity;
	private NodeColorProfile entitySet;
	private NodeColorProfile flowLine;
	private NodeColorProfile gene;
	private NodeColorProfile interactor;
	private NodeColorProfile link;
	private NodeColorProfile note;
	private NodeColorProfile otherEntity;
	private NodeColorProfile processNode;
	private NodeColorProfile protein;
	private NodeColorProfile reaction;
	private NodeColorProfile rna;
	private NodeColorProfile stoichiometry;
	private NodeColorProfile encapsulatedNode;
	private NodeColorProfile entitySetDrug;
	private NodeColorProfile proteinDrug;
	private NodeColorProfile rnaDrug;
	private NodeColorProfile complexDrug;
	private ThumbnailColorProfile thumbnail;

	public PropertiesColorProfile getProperties() {
		return properties;
	}

	public NodeColorProfile getAttachment() {
		return attachment;
	}

	public NodeColorProfile getChemical() {
		return chemical;
	}

	public NodeColorProfile getChemicalDrug() {
		return chemicalDrug;
	}

	public NodeColorProfile getCompartment() {
		return compartment;
	}

	public NodeColorProfile getComplex() {
		return complex;
	}

	public NodeColorProfile getEntity() {
		return entity;
	}

	public NodeColorProfile getEntitySet() {
		return entitySet;
	}

	public NodeColorProfile getFlowLine() {
		return flowLine;
	}

	public NodeColorProfile getGene() {
		return gene;
	}

	public NodeColorProfile getInteractor() {
		return interactor;
	}

	public NodeColorProfile getLink() {
		return link;
	}

	public NodeColorProfile getNote() {
		return note;
	}

	public NodeColorProfile getOtherEntity() {
		return otherEntity;
	}

	public NodeColorProfile getProcessNode() {
		return processNode;
	}

	public NodeColorProfile getProtein() {
		return protein;
	}

	public NodeColorProfile getReaction() {
		return reaction;
	}

	public NodeColorProfile getRna() {
		return rna;
	}

	public NodeColorProfile getStoichiometry() {
		return stoichiometry;
	}

	public NodeColorProfile getEncapsulatedNode() {
		return encapsulatedNode;
	}

	public NodeColorProfile getEntitySetDrug() {
		return entitySetDrug;
	}

	public NodeColorProfile getProteinDrug() {
		return proteinDrug;
	}

	public NodeColorProfile getRnaDrug() {
		return rnaDrug;
	}

	public NodeColorProfile getComplexDrug() {
		return complexDrug;
	}

	public ThumbnailColorProfile getThumbnail() {
		return thumbnail;
	}
}
