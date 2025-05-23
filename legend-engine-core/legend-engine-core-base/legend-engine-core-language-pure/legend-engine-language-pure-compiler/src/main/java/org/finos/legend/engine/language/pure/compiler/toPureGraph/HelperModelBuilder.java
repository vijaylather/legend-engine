// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.m3.type.Class;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity;
import org.finos.legend.engine.protocol.pure.m3.function.property.Property;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.Root_meta_external_store_model_ModelStore;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_constraint_Constraint_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_extension_TaggedValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_function_property_DefaultValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_function_property_Property_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_function_property_QualifiedProperty_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_FunctionType_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_ClassConstraintValueSpecificationContext_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_ExpressionSequenceValueSpecificationContext_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl;
import org.finos.legend.pure.generated.platform_pure_essential_meta_graph_elementToPath;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.constraint.Constraint;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecificationContext;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class HelperModelBuilder
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HelperModelBuilder.class);

    public static org.eclipse.collections.api.block.function.Function<Property, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<Object, Object>> processProperty(CompileContext context, GenericType genericType, PropertyOwner owner)
    {
        return property ->
        {
            Root_meta_pure_metamodel_function_property_DefaultValue_Impl defaultValue = null;
            if (property.defaultValue != null)
            {
                LambdaFunction<?> lambdaFunction = HelperValueSpecificationBuilder.buildLambda(Collections.singletonList(property.defaultValue.value), Collections.emptyList(), context);
                defaultValue = new Root_meta_pure_metamodel_function_property_DefaultValue_Impl(null, SourceInformationHelper.toM3SourceInformation(property.defaultValue.sourceInformation), context.pureModel.getClass("meta::pure::metamodel::function::property::DefaultValue"));
                defaultValue._functionDefinition(lambdaFunction);
            }
            GenericType returnGenericType = context.newGenericType(property.genericType);
            return new Root_meta_pure_metamodel_function_property_Property_Impl<>(property.name, SourceInformationHelper.toM3SourceInformation(property.sourceInformation), context.pureModel.getClass("meta::pure::metamodel::function::property::Property"))
                    ._name(property.name)
                    ._defaultValue(defaultValue)
                    ._classifierGenericType(context.newGenericType(context.pureModel.getType(M3Paths.Property), Lists.fixedSize.of(genericType, returnGenericType), Lists.fixedSize.of(context.pureModel.getMultiplicity(property.multiplicity))))
                    ._genericType(returnGenericType)
                    ._multiplicity(context.pureModel.getMultiplicity(property.multiplicity))
                    ._stereotypes(ListIterate.collect(property.stereotypes, s -> context.resolveStereotype(s.profile, s.value, s.profileSourceInformation, s.sourceInformation)))
                    ._taggedValues(ListIterate.collect(property.taggedValues, t -> new Root_meta_pure_metamodel_extension_TaggedValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::extension::TaggedValue"))._tag(context.resolveTag(t.tag.profile, t.tag.value, t.tag.profileSourceInformation, t.sourceInformation))._value(t.value)))
                    ._aggregation(getPropertyAggregationKindEnum(property, context))
                    ._owner(owner);
        };
    }

    private static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum getPropertyAggregationKindEnum(Property property, CompileContext context)
    {
        if (property.aggregation == null)
        {
            return null;
        }
        switch (property.aggregation)
        {
            case NONE:
            {
                return context.pureModel.getEnumValue(M3Paths.AggregationKind, "None");
            }
            case SHARED:
            {
                return context.pureModel.getEnumValue(M3Paths.AggregationKind, "Shared");
            }
            case COMPOSITE:
            {
                return context.pureModel.getEnumValue(M3Paths.AggregationKind, "Composite");
            }
            default:
            {
                throw new EngineException("Unsupported aggregation kind '" + property.aggregation + "'", property.sourceInformation, EngineErrorType.COMPILATION);
            }
        }
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression createThisVariableForClass(CompileContext context, String classPackageString)
    {
        final GenericType _classGenericType = context.resolveGenericType(classPackageString);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression ve = new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::VariableExpression"))._name("this");
        ve._genericType(_classGenericType);
        ve._multiplicity(context.pureModel.getMultiplicity(Multiplicity.PURE_ONE));
        return ve;
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification createVariableValueSpecification(CompileContext context, String variableName)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification ve = new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::VariableExpression"))._name(variableName);
        final GenericType genericType = context.newGenericType(context.pureModel.getType("Number"));
        ve._genericType(genericType);
        ve._multiplicity(context.pureModel.getMultiplicity(Multiplicity.PURE_ONE));
        return ve;
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression createVariableForMapped(LambdaFunction<?> mapFn, CompileContext context)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression ve = new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::VariableExpression"))._name("mapped");
        final GenericType genericType = context.newGenericType(((Root_meta_pure_metamodel_type_FunctionType_Impl) mapFn._classifierGenericType()._typeArguments().getFirst()._rawType())._returnType._rawType());
        ve._genericType(genericType);
        Multiplicity multiplicity = new Multiplicity();
        multiplicity.lowerBound = 0;
        ve._multiplicity(context.pureModel.getMultiplicity(multiplicity));
        return ve;
    }

    public static org.eclipse.collections.api.block.function.Function<org.finos.legend.engine.protocol.pure.m3.function.property.QualifiedProperty, QualifiedProperty<Object>> processQualifiedPropertyFirstPass(CompileContext context, PropertyOwner owner, String fullPath, ProcessingContext processingContext)
    {
        return property ->
        {
            // Remove 'this' if it has been serialized in the JSON specification (coming from an old version of the Pure serializer)
            if (!property.parameters.isEmpty() && "this".equals(property.parameters.get(0).name))
            {
                property.parameters.remove(0);
            }

            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression thisVariable = createThisVariableForClass(context, fullPath);

            return new Root_meta_pure_metamodel_function_property_QualifiedProperty_Impl<>(property.name, SourceInformationHelper.toM3SourceInformation(property.sourceInformation), null)
                    ._name(property.name)
                    ._functionName(property.name)
                    ._genericType(context.newGenericType(property.returnGenericType))
                    ._multiplicity(context.pureModel.getMultiplicity(property.returnMultiplicity))
                    ._stereotypes(ListIterate.collect(property.stereotypes, s -> context.resolveStereotype(s.profile, s.value, s.profileSourceInformation, s.sourceInformation)))
                    ._taggedValues(ListIterate.collect(property.taggedValues, t -> new Root_meta_pure_metamodel_extension_TaggedValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::extension::TaggedValue"))._tag(context.resolveTag(t.tag.profile, t.tag.value, t.tag.profileSourceInformation, t.sourceInformation))._value(t.value)))
                    ._classifierGenericType(
                            context.newGenericType(
                                    context.pureModel.getType("meta::pure::metamodel::function::property::QualifiedProperty"),
                                    Lists.fixedSize.of(
                                            PureModel.buildFunctionType(Lists.mutable.of(thisVariable).withAll(ListIterate.collect(property.parameters, p -> (VariableExpression) p.accept(new ValueSpecificationBuilder(context, Lists.mutable.empty(), processingContext)))), context.newGenericType(property.returnGenericType), context.pureModel.getMultiplicity(property.returnMultiplicity), context.pureModel)
                                    )
                            )
                    )
                    ._owner(owner);
        };
    }

    public static void checkCompatibility(CompileContext context, GenericType actualReturnType, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity actualMultiplicity, GenericType signatureType, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity signatureMultiplicity, String errorStub, SourceInformation errorSourceInformation)
    {
        checkTypeCompatibility(context, actualReturnType, signatureType, errorStub, errorSourceInformation);
        checkMultiplicityCompatibility(actualMultiplicity, signatureMultiplicity, errorStub, errorSourceInformation);
    }

    public static void checkTypeCompatibility(CompileContext context, GenericType actualReturnType, GenericType signatureType, String errorStub, SourceInformation errorSourceInformation)
    {
        ProcessorSupport ps = context.pureModel.getExecutionSupport().getProcessorSupport();
        if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericCompatibleWith(actualReturnType, signatureType, ps))//signatureType != null && !actualReturnType.equals(signatureType) && !org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(actualReturnType, signatureType, context.pureModel.getExecutionSupport().getProcessorSupport()))
        {
            throw new EngineException(errorStub + " - Type error: '" + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(actualReturnType, true, ps) + "' is not a subtype of '" + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(signatureType, true, ps) + "'", errorSourceInformation, EngineErrorType.COMPILATION);
        }
    }

    public static void checkMultiplicityCompatibility(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity actualMultiplicity, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity signatureMultiplicity, String errorStub, SourceInformation errorSourceInformation)
    {
        if (!org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.subsumes(signatureMultiplicity, actualMultiplicity))
        {
            throw new EngineException(errorStub + " - Multiplicity error: " + org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(signatureMultiplicity) + " doesn't subsume " + org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(actualMultiplicity), errorSourceInformation, EngineErrorType.COMPILATION);
        }
    }

    public static void processClassConstraints(Class srcClass, CompileContext context, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> targetClass, ProcessingContext ctx, ValueSpecification thisVariable)
    {
        ctx.addInferredVariables("this", thisVariable);
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ClassConstraintValueSpecificationContext classConstraintValueSpecificationContext = new Root_meta_pure_metamodel_valuespecification_ClassConstraintValueSpecificationContext_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::ClassConstraintValueSpecificationContext"))._type(targetClass);
        RichIterable<Constraint> pureConstraints = processConstraints(srcClass.constraints, context, srcClass._package, srcClass.name, classConstraintValueSpecificationContext, ctx);
        ctx.flushVariable("this");
        targetClass._constraints(pureConstraints);
    }

    public static void processFunctionConstraints(Function f, CompileContext context, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition<?> targetFunc, ProcessingContext ctx)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ExpressionSequenceValueSpecificationContext expressionSequenceValueSpecificationContext = new Root_meta_pure_metamodel_valuespecification_ExpressionSequenceValueSpecificationContext_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::ExpressionSequenceValueSpecificationContext"))._functionDefinition(targetFunc);

        RichIterable<Constraint> functionPreConstraints = processConstraints(f.preConstraints, context, f._package, f.name, expressionSequenceValueSpecificationContext, ctx);
        RichIterable<Constraint> functionPostConstraints = processConstraints(f.postConstraints, context, f._package, f.name, expressionSequenceValueSpecificationContext, ctx);
        targetFunc._preConstraints(functionPreConstraints);
        targetFunc._postConstraints(functionPostConstraints);
    }

    public static RichIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.constraint.Constraint> processConstraints(List<org.finos.legend.engine.protocol.pure.m3.constraint.Constraint> constraints, CompileContext context, String _package, String _name, ValueSpecificationContext vsContext, ProcessingContext ctx)
    {
        String constraintSourceId = context.pureModel.buildPackageString(_package, _name).replace("::", "_") + "_Constraint$";

        return ListIterate.collect(constraints, c ->
        {
            ctx.push("Constraint " + c.name);
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?> lf;
            try
            {
                lf = HelperValueSpecificationBuilder.buildLambdaWithContext(c.name, c.functionDefinition.body, c.functionDefinition.parameters, context, ctx);
                lf._expressionSequence().forEach(es -> es._usageContext(vsContext));
                lf.setSourceInformation(SourceInformationHelper.toM3SourceInformation(c.functionDefinition.sourceInformation));
            }
            catch (Exception e)
            {
                LOGGER.warn(new LogInfo(Identity.getAnonymousIdentity().getName(), LoggingEventType.GRAPH_EXPRESSION_ERROR, "Can't build constraint '" + c.name + "' of class '" + context.pureModel.buildPackageString(_package, _name) + "- stack: " + ctx.getStack()).toString());
                if (e instanceof EngineException)
                {
                    throw e;
                }
                throw new EngineException(e.getMessage(), c.sourceInformation, EngineErrorType.COMPILATION);
            }
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction<?> mf = null;
            try
            {
                if (c.messageFunction != null)
                {
                    mf = HelperValueSpecificationBuilder.buildLambdaWithContext(c.name, c.messageFunction.body, c.messageFunction.parameters, context, ctx);
                    mf._expressionSequence().forEach(es -> es._usageContext(vsContext));
                    mf.setSourceInformation(SourceInformationHelper.toM3SourceInformation(c.messageFunction.sourceInformation));
                }
            }
            catch (Exception e)
            {
                mf = null;
                LOGGER.warn(new LogInfo(Identity.getAnonymousIdentity().getName(), LoggingEventType.GRAPH_EXPRESSION_ERROR, "Can't build the message function for constraint '" + c.name + "' of class '" + context.pureModel.buildPackageString(_package, _name) + "' - stack: " + ctx.getStack()).toString());
                /* We let these through as a warning because there are invalid message functions that are not properly caught by the PURE compiler .
                   For example:
                      ~message: 'String ' + $this.maybe
                      where maybe is String[0..1]

                   which should cause a multiplicity error as arguments to plus should all be [1] (to become elements of to plus(parts:String[*]))
                   In the absence of a message the system will fall back on the default failure message.
                 */
            }
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.constraint.Constraint pureConstraint = new Root_meta_pure_metamodel_constraint_Constraint_Impl(constraintSourceId, SourceInformationHelper.toM3SourceInformation(c.sourceInformation), context.pureModel.getClass("meta::pure::metamodel::constraint::Constraint"));
            pureConstraint.setSourceInformation(SourceInformationHelper.toM3SourceInformation(c.sourceInformation));
            pureConstraint._functionDefinition(lf);
            pureConstraint._name(c.name);
            pureConstraint._externalId(c.externalId);
            pureConstraint._enforcementLevel(c.enforcementLevel);
            pureConstraint._messageFunction(mf);
            ctx.pop();

            if (!lf._expressionSequence().getLast()._genericType()._rawType().equals(context.pureModel.getType("Boolean")))
            {
                throw new EngineException("Constraint must be of type 'Boolean'", c.functionDefinition.body.get(c.functionDefinition.body.size() - 1).sourceInformation, EngineErrorType.COMPILATION);
            }
            return pureConstraint;
        });
    }

    /**
     * Generate function signature so we can have an ID for function in the graph. This way we can have function of the same name.
     */
    public static String getSignature(Function function)
    {
        return getFunctionNameWithoutSignature(function) + terseSignatureSuffix(function);
    }

    public static String getFunctionNameWithoutSignature(Function function)
    {
        String signaureSuffix = terseSignatureSuffix(function);
        return function.name.endsWith(signaureSuffix) ? function.name.substring(0, function.name.length() - signaureSuffix.length()) : function.name;
    }


    public static String getTerseSignature(Function function)
    {
        String suffix = terseSignatureSuffix(function);
        //A function name may or may not already have a signature. Only add it if it's not present
        return function.name.endsWith(suffix) ? function.name : function.name + suffix;
    }

    private static String terseSignatureSuffix(Function function)
    {
        String functionSignature = LazyIterate.collect(function.parameters, HelperModelBuilder::getParameterSignature).select(Objects::nonNull).makeString("__")
                // TODO: do we have to take care of void return type ~ Nil?
                + "__" + getClassSignature(((PackageableType) function.returnGenericType.rawType).fullPath) + "_" + getMultiplicitySignature(function.returnMultiplicity) + "_";
        return function.parameters.isEmpty() ? functionSignature : ("_" + functionSignature);
    }


    private static String getParameterSignature(Variable p)
    {
        return p.genericType != null ? getClassSignature(((PackageableType) p.genericType.rawType).fullPath) + "_" + getMultiplicitySignature(p.multiplicity) : null;
    }

    private static String getClassSignature(String _class)
    {
        if (_class == null)
        {
            return null;
        }
        return _class.contains("::") ? _class.substring(_class.lastIndexOf("::") + 2) : _class;
    }

    private static String getMultiplicitySignature(Multiplicity multiplicity)
    {
        if (multiplicity.lowerBound == multiplicity.getUpperBoundInt())
        {
            return "" + multiplicity.lowerBound;
        }
        else if (multiplicity.lowerBound == 0 && multiplicity.getUpperBoundInt() == Integer.MAX_VALUE)
        {
            return "MANY";
        }
        return "$" + multiplicity.lowerBound + "_" + (multiplicity.getUpperBoundInt() == Integer.MAX_VALUE ? "MANY" : multiplicity.getUpperBoundInt()) + "$";
    }

    /**
     * Find the normal/non-derived property that the class owns.
     */
    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> getOwnedProperty(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class, final String name, CompiledExecutionSupport executionSupport)
    {
        return getOwnedProperty(_class, name, SourceInformation.getUnknownSourceInformation(), executionSupport);
    }

    /**
     * Find the normal/non-derived property that the class owns.
     */
    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> getOwnedProperty(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class, final String name, SourceInformation sourceInformation, CompiledExecutionSupport executionSupport)
    {
        return HelperModelBuilder.getOwnedProperty(_class, null, name, sourceInformation, executionSupport);
    }

    /**
     * Find the normal/non-derived property that the class owns.
     */
    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> getOwnedProperty(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class, String classPath, final String name, SourceInformation sourceInformation, CompiledExecutionSupport executionSupport)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> property = _class._properties().detect(p -> name.equals(p.getName()));
        if (property == null)
        {
            property = _class._propertiesFromAssociations().detect(p -> name.equals(p.getName()));
        }
        Assert.assertTrue(property != null, () -> "Can't find property '" + name + "' in class '" + (classPath != null ? classPath : getElementFullPath(_class, executionSupport)) + "'", sourceInformation, EngineErrorType.COMPILATION);
        return property;
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> getAssociationProperty(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association _association, final String name, SourceInformation sourceInformation, CompileContext context)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> property = _association._properties().detect(p -> name.equals(p.getName()));
        Assert.assertTrue(property != null, () -> "Can't find property '" + name + "' in association '" + (getElementFullPath(_association, context.pureModel.getExecutionSupport())) + "'", sourceInformation, EngineErrorType.COMPILATION);
        return property;
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> getAssociationPropertyClass(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association _association, final String name, SourceInformation sourceInformation, CompileContext context)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> property = _association._properties().detect(p -> !name.equals(p.getName()));
        Assert.assertTrue(property != null, () -> "Can't find associated property of property '" + name + "' in association '" + (getElementFullPath(_association, context.pureModel.getExecutionSupport())) + "'", sourceInformation, EngineErrorType.COMPILATION);
        return (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) property._genericType()._rawType();
    }

    public static AbstractProperty<?> getOwnedAppliedProperty(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class, String name, SourceInformation sourceInformation, CompiledExecutionSupport executionSupport)
    {
        return HelperModelBuilder.getOwnedAppliedProperty(_class, null, name, sourceInformation, executionSupport);
    }

    /**
     * Find the property (normal and derived) that the class owns.
     */
    public static AbstractProperty<?> getOwnedAppliedProperty(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class, String classPath, String name, SourceInformation sourceInformation, CompiledExecutionSupport executionSupport)
    {
        AbstractProperty<?> prop = _class._properties().detect(p -> name.equals(p.getName()));
        if (prop == null)
        {
            prop = _class._propertiesFromAssociations().detect(p -> name.equals(p._name()));
        }
        if (prop == null)
        {
            prop = _class._qualifiedProperties().detect(p -> name.equals(p._name()));
        }
        if (prop == null)
        {
            prop = _class._qualifiedPropertiesFromAssociations().detect(p -> name.equals(p._name()));
        }
        Assert.assertTrue(prop != null, () -> "Can't find property '" + name + "' in class '" + (classPath != null ? classPath : getElementFullPath(_class, executionSupport)) + "'", sourceInformation, EngineErrorType.COMPILATION);
        return prop;
    }

    /**
     * Find the property (normal and derived) that the a property owner (class or association) owns.
     */
    public static AbstractProperty<?> getAllOwnedAppliedProperty(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner propertyOwner, String name, SourceInformation sourceInformation, CompiledExecutionSupport executionSupport)
    {
        AbstractProperty<?> prop = null;
        if (propertyOwner instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class)
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) propertyOwner;
            prop = _class._properties().detect(p -> name.equals(p.getName()));
            if (prop == null)
            {
                prop = _class._propertiesFromAssociations().detect(p -> name.equals(p._name()));
            }
            if (prop == null)
            {
                prop = _class._qualifiedProperties().detect(p -> name.equals(p._name()));
            }
            if (prop == null)
            {
                prop = _class._qualifiedPropertiesFromAssociations().detect(p -> name.equals(p._name()));
            }
        }
        else if (propertyOwner instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association)
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association _association = (Association) propertyOwner;
            prop = _association._properties().detect(p -> name.equals(p.getName()));
        }
        String propertyOwnerType = propertyOwner instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class ? "class" : "association";
        Assert.assertTrue(prop != null, () -> "Can't find property '" + name + "' in " + propertyOwnerType + " '" + (getElementFullPath(propertyOwner, executionSupport)) + "'", sourceInformation, EngineErrorType.COMPILATION);
        return prop;
    }

    /**
     * Recursively go through hierarchical/generalization chain and find the property and resolve to edge point property for milestoned properties.
     */
    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> getPropertyOrResolvedEdgePointProperty(CompileContext context, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class, Optional<? extends List<? extends org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification>> parameters, String name, SourceInformation sourceInformation)
    {
        return getPropertyOrResolvedEdgePointProperty(context, _class, parameters, name, false, sourceInformation);
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> getPropertyOrResolvedEdgePointProperty(CompileContext context, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class, Optional<? extends List<? extends org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification>> parameters, String name, boolean excludeQualified, SourceInformation sourceInformation)
    {
        AbstractProperty<?> abstractProperty = HelperModelBuilder.getAppliedProperty(context, _class, parameters, name, excludeQualified, sourceInformation);
        if ((abstractProperty instanceof QualifiedProperty) && Milestoning.temporalStereotypes(((PackageableElement) abstractProperty._genericType()._rawType())._stereotypes()) != null)
        {
            return (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?>) HelperModelBuilder.getAppliedProperty(context, _class, parameters, MilestoningFunctions.getEdgePointPropertyName(name), excludeQualified, sourceInformation);
        }
        return (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?>) abstractProperty;
    }

    /**
     * Recursively go through hierarchical/generalization chain and find the property.
     */
    public static AbstractProperty<?> getAppliedProperty(CompileContext context, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class, Optional<? extends List<? extends org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification>> parameters, String name)
    {
        return getAppliedProperty(context, _class, parameters, name, false, SourceInformation.getUnknownSourceInformation());
    }

    public static AbstractProperty<?> getAppliedProperty(CompileContext context, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class, Optional<? extends List<? extends org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification>> parameters, String name, SourceInformation sourceInformation)
    {
        return getAppliedProperty(context, _class, parameters, name, false, sourceInformation);
    }

    /**
     * Recursively go through hierarchical/generalization chain and find the property.
     */
    public static AbstractProperty<?> getAppliedProperty(CompileContext context, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class, Optional<? extends List<? extends org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification>> parameters, String name, boolean excludeQualified, SourceInformation sourceInformation)
    {
        for (CoreInstance c_type : org.finos.legend.pure.m3.navigation.type.Type.getGeneralizationResolutionOrder(_class, context.pureModel.getExecutionSupport().getProcessorSupport()))
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> type = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) c_type;
            AbstractProperty<?> property = type._properties().detect(p -> name.equals(p.getName()));
            if (property != null)
            {
                return property;
            }
            property = type._propertiesFromAssociations().detect(p -> name.equals(p.getName()));
            if (property != null)
            {
                return property;
            }
            property = getCompatibleDerivedProperty(type._qualifiedProperties(), name, parameters, excludeQualified);
            if (property != null)
            {
                return property;
            }
            property = type._qualifiedPropertiesFromAssociations().detect(p -> name.equals(p._name()));
            if (property != null)
            {
                return property;
            }
        }
        throw new EngineException("Can't find property '" + name + "' in [" + org.finos.legend.pure.m3.navigation.type.Type.getGeneralizationResolutionOrder(_class, context.pureModel.getExecutionSupport().getProcessorSupport()).makeString(", ") + "]", sourceInformation, EngineErrorType.COMPILATION);
    }

    public static QualifiedProperty<?> getCompatibleDerivedProperty(RichIterable<? extends QualifiedProperty<?>> qualifiedProperties, String name, Optional<? extends List<? extends org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification>> parameters, boolean excludeQualified)
    {
        RichIterable<? extends QualifiedProperty<?>> propertiesWithSameName = qualifiedProperties.select(p -> name.equals(p._name()));
        RichIterable<? extends QualifiedProperty<?>> milestoningProperties = propertiesWithSameName.select(Milestoning::isDateArgGeneratedMilestoningQualifiedProperty);                             //dont pick NoArg Milestoned Property

        if (!milestoningProperties.isEmpty())
        {
            return milestoningProperties.getFirst();
        }
        else if (excludeQualified)
        {
            return null;
        }
        else
        {
            if (parameters.isPresent())
            {
                return propertiesWithSameName.detect(p -> isCompatibleDerivedPropertyWithParameters(p, parameters.get()));
            }
            else
            {
                return propertiesWithSameName.getFirst();
            }
        }
    }

    public static boolean isCompatibleDerivedPropertyWithParameters(QualifiedProperty<?> o, List<? extends org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification> params)
    {
        FunctionType rawType = (FunctionType) o._classifierGenericType()._typeArguments().getFirst()._rawType();
        return rawType._parameters().size() == params.size();
//                && rawType._parameters().zip(params).allSatisfy(v1 -> v1.getOne()._name().equals(((Variable) v1.getTwo()).name));
//        FIXME: we might need to be smarter about which property to choose (use types for example)
//        Line 105 won't work for buildLambda (but will work when deserializing and instantiating PureModel).
//        The reason lambda construction fails: take {| Person.all()->filter(p|$p.nameWithTitle('Mr') == 'Mr John Smith')} as an example:
//        when processing $p.nameWithTitle('Mr'), the parameter name is p, but the AppliedQualfiedProperty has name 'this' and thus the comparison on parameter names fails.
    }

    public static boolean isCompatibleDerivedProperty(QualifiedProperty<?> o, org.finos.legend.engine.protocol.pure.m3.function.property.QualifiedProperty p)
    {
        return o._name().equals(p.name) && isCompatibleDerivedPropertyWithParameters(o, Lists.mutable.of(new Variable()).withAll(p.parameters));
    }

    public static String getTypeFullPath(Type type, CompiledExecutionSupport executionSupport)
    {
        return getTypeFullPath(type, "::", executionSupport);
    }

    public static String getTypeFullPath(Type type, String separator, CompiledExecutionSupport executionSupport)
    {
        if (type instanceof PackageableElement)
        {
            return getElementFullPath((PackageableElement) type, separator, executionSupport);
        }
        if (type instanceof Unit)
        {
            return Measure.getUserPathForUnit(type, separator);
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    public static String getElementFullPath(PackageableElement element, CompiledExecutionSupport executionSupport)
    {
        return getElementFullPath(element, "::", executionSupport);
    }

    public static String getElementFullPath(PackageableElement element, String separator, CompiledExecutionSupport executionSupport)
    {
        if (element instanceof Root_meta_external_store_model_ModelStore)
        {
            return "ModelStore";
        }
        // TODO: we might want to fix a bugs here where if we pass in element without ID/package + name, we might get `cannot cast a collection of multiplicity [0] to [1]` or so
        return platform_pure_essential_meta_graph_elementToPath.Root_meta_pure_functions_meta_elementToPath_PackageableElement_1__String_1__String_1_(element, separator, executionSupport);
    }
}
