package com.fpwt.service.beans;

/**
 * @author c-NikunjP
 *
 */
public class GoalDetails {

	private String goalName;
	private String tenor;
	private String presentValue;
	private String goalCategory;
	private String recurringGoalFreq;
	private boolean recurringGoal;

	public String getGoalName() {
		return goalName;
	}

	public void setGoalName(String goalName) {
		this.goalName = goalName;
	}

	public String getTenor() {
		return tenor;
	}

	public void setTenor(String tenor) {
		this.tenor = tenor;
	}

	public String getPresentValue() {
		return presentValue;
	}

	public void setPresentValue(String presentValue) {
		this.presentValue = presentValue;
	}

	public String getGoalCategory() {
		return goalCategory;
	}

	public void setGoalCategory(String goalCategory) {
		this.goalCategory = goalCategory;
	}

	public String getRecurringGoalFreq() {
		return recurringGoalFreq;
	}

	public void setRecurringGoalFreq(String recurringGoalFreq) {
		this.recurringGoalFreq = recurringGoalFreq;
	}

	public boolean isRecurringGoal() {
		return recurringGoal;
	}

	public void setRecurringGoal(boolean recurringGoal) {
		this.recurringGoal = recurringGoal;
	}

	@Override
	public String toString() {
		return "GoalDetails [goalName=" + goalName + ", tenor=" + tenor + ", presentValue=" + presentValue
				+ ", goalCategory=" + goalCategory + ", recurringGoalFreq=" + recurringGoalFreq + ", recurringGoal="
				+ recurringGoal + "]";
	}

}
