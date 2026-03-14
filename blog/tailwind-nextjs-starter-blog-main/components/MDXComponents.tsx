import TOCInline from 'pliny/ui/TOCInline'
import Pre from 'pliny/ui/Pre'
import BlogNewsletterForm from 'pliny/ui/BlogNewsletterForm'
import type { MDXComponents } from 'mdx/types'
import { Children, Fragment, type ReactNode } from 'react'
import Image from './Image'
import CustomLink from './Link'
import TableWrapper from './TableWrapper'

const formatBoldText = (text: string, keyPrefix: string) => {
  const pattern = /\*\*(.+?)\*\*/g
  const nodes: ReactNode[] = []
  let start = 0
  let index = 0
  let match = pattern.exec(text)

  while (match) {
    if (match.index > start) {
      nodes.push(text.slice(start, match.index))
    }
    nodes.push(<strong key={`${keyPrefix}-${index}`}>{match[1]}</strong>)
    start = pattern.lastIndex
    index += 1
    match = pattern.exec(text)
  }

  if (start < text.length) {
    nodes.push(text.slice(start))
  }

  return nodes.length > 0 ? nodes : text
}

const formatInlineMarkdown = (children: ReactNode) =>
  Children.toArray(children).map((child, index) => {
    if (typeof child !== 'string') {
      return child
    }

    const formatted = formatBoldText(child, `inline-${index}`)
    return <Fragment key={`fragment-${index}`}>{formatted}</Fragment>
  })

export const components: MDXComponents = {
  Image,
  TOCInline,
  a: CustomLink,
  pre: Pre,
  table: TableWrapper,
  p: ({ children, ...props }) => <p {...props}>{formatInlineMarkdown(children)}</p>,
  li: ({ children, ...props }) => <li {...props}>{formatInlineMarkdown(children)}</li>,
  BlogNewsletterForm,
}
